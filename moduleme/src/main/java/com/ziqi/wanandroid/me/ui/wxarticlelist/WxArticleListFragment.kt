package com.ziqi.wanandroid.me.ui.wxarticlelist

import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.ImageView
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
import com.ziqi.wanandroid.me.R
import com.ziqi.wanandroid.commonlibrary.bean.ListProject
import com.ziqi.wanandroid.me.databinding.FragmentWxArticleListBinding
import com.ziqi.wanandroid.commonlibrary.ui.common.BaseFragment
import com.ziqi.wanandroid.commonlibrary.ui.imagepreview.ImagePreviewParams
import com.ziqi.wanandroid.commonlibrary.util.imageload.ImageLoadUtils
import com.ziqi.wanandroid.commonlibrary.util.StartUtil
import com.ziqi.wanandroid.commonlibrary.view.ImageViewX

class WxArticleListFragment :
    BaseFragment<WxArticleListViewModel, WxArticleListParams, FragmentWxArticleListBinding>() {

    companion object {
        @JvmStatic
        fun newInstance(bundle: Bundle?) = WxArticleListFragment().apply {
            arguments = bundle
        }
    }


    private var mAdapter: BaseQuickAdapter<ListProject.DatasBean, BaseViewHolder>? = null

    private var mCurrentData: ListProject? = null


    override fun zContentViewId(): Int {
        return R.id.myRootView
    }

    override fun zSetLayoutId(): Int {
        return R.layout.fragment_wx_article_list
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

    override fun initView() {
        mAdapter =
            object : BaseQuickAdapter<ListProject.DatasBean, BaseViewHolder>(
                R.layout.fragment_recent_project_item,
                null
            ) {
                override fun convert(holder: BaseViewHolder, item: ListProject.DatasBean) {
                    holder.setText(
                        R.id.author,
                        StringUtil.emptyTip(
                            item.author,
                            item.shareUser ?: getString(R.string.common_no_info)
                        )
                    )
                    holder.setText(
                        R.id.desc,
                        StringUtil.emptyTip(
                            Html.fromHtml(item.desc).toString(),
                            getString(R.string.common_no_introduction)
                        )
                    )
                    holder.setText(R.id.title, Html.fromHtml(item.title))
                    holder.setText(
                        R.id.niceDate,
                        String.format(
                            getString(R.string.common_with_time_tip),
                            item.niceDate?.trim()
                        )
                    )
                    holder.setText(
                        R.id.chapterName, """${item.chapterName}/${item.superChapterName}"""
                    )

                    //控制是否展示图片就好
                    holder.getView<ImageViewX>(R.id.envelopePic).visibility =
                        if (StringUtil.isEmpty(item.envelopePic)) View.GONE else View.VISIBLE

                    ImageLoadUtils.loadUrl(
                        this@WxArticleListFragment,
                        item.envelopePic,
                        holder.getView(R.id.envelopePic),
                        R.drawable.icon_placeholder,
                        ImageView.ScaleType.FIT_CENTER
                    )

                    holder.getView<LinearLayout>(R.id.content).setOnClickListener {
                        activity?.let {
                            val webInfo = WebInfo()
                            webInfo.url = item.link
                            StartUtil.startWebFragment(it, this@WxArticleListFragment, -1, webInfo)
                        }
                    }
                    holder.getView<ImageViewX>(R.id.envelopePic).setOnClickListener {
                        activity?.let {
                            val params =
                                ImagePreviewParams()
                            params.imgUrl = arrayListOf(item.envelopePic)
                            StartUtil.startImagePreviewFragment(
                                it,
                                this@WxArticleListFragment,
                                -1,
                                params
                            )
                        }
                    }
                    holder.getView<ImageView>(R.id.ivCollect).isSelected = "true" == item.collect
                    holder.getView<LinearLayout>(R.id.llCollect).setOnClickListener {
                        toLogin(object : LoginListener {
                            override fun onSuccess() {

                            }

                            override fun onCancel() {
                            }

                        }, null)
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
            val curPage = mCurrentData?.curPage ?: 1
            val pageCount = mCurrentData?.pageCount ?: 1
            if (curPage >= pageCount) {
                mAdapter?.loadMoreEnd()
            } else {
                //这从0开始，只能加1
                mViewModel?.wxArticleList(curPage + 1, mStartParams?.tree?.id ?: 0)
            }
        }, mViewDataBinding?.recyclerview)
        mViewDataBinding?.myRootView?.setOnRefreshListener(this)
    }

    override fun dealViewModel() {
        mViewModel?.mRefresh?.observe(viewLifecycleOwner, Observer {
            mViewDataBinding?.myRootView?.isRefreshing = false
        })
        mViewModel?.mLoadMore?.observe(viewLifecycleOwner, Observer {
            it.apply {
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
        mViewModel?.mListProject?.observe(viewLifecycleOwner, Observer {
            it?.apply {
                datas?.apply {
                    if (curPage <= 1) {
                        mAdapter?.setNewData(this)
                    } else {
                        mAdapter?.addData(this)
                    }
                }
                mAdapter?.setEnableLoadMore(pageCount > 1)
                mCurrentData = this
            }
        })
    }


    override fun onRefresh() {
        mViewModel?.wxArticleList(0, mStartParams?.tree?.id ?: 0)
    }


}
