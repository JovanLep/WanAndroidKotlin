package com.ziqi.wanandroid.ui.recentblog

import android.os.Parcelable
import android.text.Html
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.animation.AlphaInAnimation
import com.ziqi.baselibrary.common.WebInfo
import com.ziqi.baselibrary.util.StringUtil
import com.ziqi.baselibrary.view.status.ZStatusViewBuilder
import com.ziqi.wanandroid.R
import com.ziqi.wanandroid.bean.Article
import com.ziqi.wanandroid.bean.WanList
import com.ziqi.wanandroid.databinding.FragmentRecentBlogBinding
import com.ziqi.wanandroid.ui.common.BaseFragment
import com.ziqi.wanandroid.util.StartUtil

class RecentBlogFragment :
    BaseFragment<RecentBlogViewModel, Parcelable, FragmentRecentBlogBinding>() {

    companion object {
        fun newInstance() = RecentBlogFragment()
    }


    private var mAdapter: BaseQuickAdapter<Article, BaseViewHolder>? = null

    var mData: WanList<Article>? = null


    override fun zSetLayoutId(): Int {
        return R.layout.fragment_recent_blog
    }

    override fun zContentViewId(): Int {
        return R.id.myRootView
    }

    override fun onClick(v: View?) {

    }

    override fun zVisibleToUser(isNewIntent: Boolean) {
    }

    override fun zLazyVisible() {
        super.zLazyVisible()
        mZStatusView?.config(
            ZStatusViewBuilder.Builder()
                .setOnErrorRetryClickListener {
                    zStatusLoadingView()
                    onRefresh()
                }
                .setOnEmptyRetryClickListener {
                    zStatusLoadingView()
                    onRefresh()
                }
                .build())
        initView()
        dealViewModel()
        onRefresh()
    }

    override fun onRefresh() {
        mViewModel?.loadArticleTop(false)
    }

    override fun initView() {
        mAdapter =
            object : BaseQuickAdapter<Article, BaseViewHolder>(
                R.layout.fragment_recent_blog_item,
                null
            ) {
                override fun convert(holder: BaseViewHolder, item: Article) {
                    holder.setText(
                        R.id.author,
                        StringUtil.emptyTip(item.author, item.shareUser ?: "暂无")
                    )
                    holder.setText(R.id.title, Html.fromHtml(item.title))
                    holder.setText(R.id.niceDate, """时间：${item.niceDate?.trim()}""")
                    holder.setText(
                        R.id.chapterName, """${item.chapterName}/${item.superChapterName}"""
                    )
                    holder.getView<LinearLayout>(R.id.content).setOnClickListener {
                        activity?.let {
                            val webInfo = WebInfo()
                            webInfo.url = item.link
                            StartUtil.startWebFragment(it, this@RecentBlogFragment, -1, webInfo)
                        }
                    }
                }
            }

        mAdapter?.openLoadAnimation(AlphaInAnimation())
        mViewDataBinding?.recyclerview?.adapter = mAdapter
        //=================================================================================
        mViewDataBinding?.recyclerview?.layoutManager = LinearLayoutManager(context)
        mViewDataBinding?.recyclerview?.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
        mAdapter?.setOnItemClickListener { _, _, _ ->

        }
        //https://github.com/CymChad/BaseRecyclerViewAdapterHelper/blob/master/readme/8-LoadMore.md
        mAdapter?.setOnLoadMoreListener({
            val curPage = mData?.curPage
            val pageCount = mData?.pageCount
            if (curPage ?: 1 >= pageCount ?: 1) {
                mAdapter?.loadMoreEnd()
            } else {
                mViewModel?.loadArticleList(mData?.curPage ?: 1)
            }
        }, mViewDataBinding?.recyclerview)
        mViewDataBinding?.myRootView?.setOnRefreshListener(this)
    }

    override fun dealViewModel() {
        mViewModel?.mRefresh?.observe(viewLifecycleOwner, Observer {
            mViewDataBinding?.myRootView?.isRefreshing = false
        })
        mViewModel?.mLoadMore?.observe(viewLifecycleOwner, Observer {
            it?.apply {
                when (getContentIfNotHandled()) {
                    true -> {
                        mAdapter?.loadMoreComplete()
                    }
                    false -> {
                        mAdapter?.loadMoreFail()
                    }
                }
            }
        })
        mViewModel?.mArticleTop?.observe(viewLifecycleOwner, Observer {
            mAdapter?.setNewData(it)
        })
        mViewModel?.mArticleList?.observe(viewLifecycleOwner, Observer {
            it?.let {
                it.datas?.apply {
                    if (it.curPage == 1) {
                        mAdapter?.setNewData(this)
                    } else {
                        mAdapter?.addData(this)
                    }
                }
                mAdapter?.setEnableLoadMore(it.pageCount > 1)
                mData = it
            }
        })
    }
}
