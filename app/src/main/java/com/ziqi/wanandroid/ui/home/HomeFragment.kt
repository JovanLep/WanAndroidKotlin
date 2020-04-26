package com.ziqi.wanandroid.ui.home

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.animation.AlphaInAnimation
import com.chad.library.adapter.base.module.BaseLoadMoreModule
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.youth.banner.config.IndicatorConfig
import com.youth.banner.indicator.CircleIndicator
import com.youth.banner.listener.OnPageChangeListener
import com.youth.banner.transformer.DepthPageTransformer
import com.youth.banner.util.BannerUtils
import com.ziqi.baselibrary.base.ZBaseFragment
import com.ziqi.baselibrary.common.WebInfo
import com.ziqi.baselibrary.livedata.Event
import com.ziqi.baselibrary.mvvm.ViewModelFragment
import com.ziqi.baselibrary.util.StringUtil
import com.ziqi.baselibrary.view.status.ZStatusView
import com.ziqi.baselibrary.view.status.ZStatusViewBuilder
import com.ziqi.wanandroid.R
import com.ziqi.wanandroid.bean.Article
import com.ziqi.wanandroid.bean.Banner
import com.ziqi.wanandroid.bean.WanResponseList
import com.ziqi.wanandroid.databinding.FragmentHomeBinding
import com.ziqi.wanandroid.databinding.FragmentHomeHeaderBinding
import com.ziqi.wanandroid.ui.main.MainViewModel
import com.ziqi.wanandroid.util.StartUtil
import com.ziqi.wanandroid.view.banner.ImageAdapter


/**
 * Copyright (C), 2018-2020
 * Author: ziqimo
 * Date: 2020/4/15 11:37 AM
 * Description:
 * History:
 * <author> <time> <version> <desc>
 * 作者姓名 修改时间 版本号 描述
 */
class HomeFragment : ViewModelFragment<HomeViewModel, Parcelable, FragmentHomeBinding>(),
    SwipeRefreshLayout.OnRefreshListener {

    companion object {
        @JvmStatic
        fun newInstance(bundle: Bundle?): HomeFragment {
            var mWBaseFragment = HomeFragment()
            mWBaseFragment.arguments = bundle
            return mWBaseFragment
        }
    }

    private var mHeaderViewDataBinding: FragmentHomeHeaderBinding? = null

    private lateinit var mAdapter: BaseQuickAdapter<Article, BaseViewHolder>

    override fun onClick(v: View?) {

    }

    override fun zSetLayoutId(): Int {
        return R.layout.fragment_home
    }

    override fun zContentViewId(): Int {
        return R.id.myRootView
    }

    override fun zVisibleToUser(isNewIntent: Boolean) {

    }

    override fun zLazyVisible() {
        super.zLazyVisible()
        initRv()
        mZStatusView?.config(ZStatusViewBuilder.Builder()
            .setOnErrorRetryClickListener {
                zStatusLoadingView()
                onRefresh()
            }
            .setOnEmptyRetryClickListener {
                zStatusLoadingView()
                onRefresh()
            }
            .build())

        mViewModel?.mBanner?.observe(viewLifecycleOwner, Observer {
            mHeaderViewDataBinding?.banner?.apply {
                setAdapter(ImageAdapter(it))
                setIndicator(CircleIndicator(context))
                setIndicatorSelectedColorRes(R.color.colorPrimary)
                setIndicatorNormalColorRes(R.color.color_999999)
                setIndicatorGravity(IndicatorConfig.Direction.LEFT)
                setIndicatorSpace(BannerUtils.dp2px(20f).toInt())
                setIndicatorMargins(IndicatorConfig.Margins(BannerUtils.dp2px(10f).toInt()))
                setIndicatorWidth(10, 20)
                setPageTransformer(DepthPageTransformer())
                setOnBannerListener { data, position ->
                    activity?.let {
                        val webInfo = WebInfo()
                        webInfo.url = (data as Banner).url
                        StartUtil.startWebFragment(it, this@HomeFragment, -1, webInfo)
                    }
                }
                addOnPageChangeListener(object : OnPageChangeListener {
                    override fun onPageScrollStateChanged(state: Int) {
                    }

                    override fun onPageScrolled(
                        position: Int,
                        positionOffset: Float,
                        positionOffsetPixels: Int
                    ) {
                    }

                    override fun onPageSelected(position: Int) {
                        if (position >= 0 && position < it.size) {
                            val banner = it.get(position)
                            mHeaderViewDataBinding?.title?.text = banner.title
                        }
                    }

                })
                start()
            }
        })
        mViewModel?.mArticleTop?.observe(viewLifecycleOwner, Observer {
            mAdapter.setNewInstance(it)
            mViewDataBinding?.myRootView?.isRefreshing = false
        })
        mViewModel?.mArticleList?.observe(viewLifecycleOwner, Observer {
            mAdapter.loadMoreModule.isEnableLoadMore = it.curPage < it.pageCount
            it.datas?.apply {
                mAdapter.addData(this)
            }
        })
        mViewModel?.loadArticleTop(false)
    }

    private fun initRv() {
        mAdapter =
            object : BaseQuickAdapter<Article, BaseViewHolder>(R.layout.fragment_home_item, null) {
                override fun convert(holder: BaseViewHolder, item: Article) {
                    holder.setText(R.id.author, StringUtil.emptyTip(item.author, "暂无"))
                    holder.setText(R.id.title, item.title)
                    holder.setText(R.id.niceDate, """时间：${item.niceDate}""")
                    holder.setText(
                        R.id.chapterName, """${item.chapterName}/${item.superChapterName}"""
                    )
                }
            }

        mAdapter.adapterAnimation = AlphaInAnimation()
        mViewDataBinding?.recyclerview?.adapter = mAdapter
        mViewDataBinding?.recyclerview?.layoutManager = LinearLayoutManager(context)
        mViewDataBinding?.recyclerview?.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
        mAdapter.setOnItemClickListener { _, _, _ ->

        }
        val headerView = LayoutInflater.from(context).inflate(R.layout.fragment_home_header, null)
        mHeaderViewDataBinding = DataBindingUtil.bind(headerView)
        mAdapter.addHeaderView(headerView)
        mAdapter.headerWithEmptyEnable = true
        mAdapter.addLoadMoreModule(object : LoadMoreAdapter<Article>(-1, null) {
            override fun convert(holder: BaseViewHolder, item: Article) {
            }
        })
        //https://github.com/CymChad/BaseRecyclerViewAdapterHelper/blob/master/readme/8-LoadMore.md
        mAdapter.loadMoreModule.setOnLoadMoreListener {
            val list = mViewModel?.mArticleList?.value
            mViewModel?.loadArticleList(list?.curPage ?: 1)
        }
        mAdapter.loadMoreModule.isEnableLoadMore = false
        mViewDataBinding?.myRootView?.setOnRefreshListener(this)
    }


    override fun onResume() {
        super.onResume()
        mHeaderViewDataBinding?.banner?.start()
    }

    override fun onPause() {
        super.onPause()
        mHeaderViewDataBinding?.banner?.stop()
    }

    override fun onRefresh() {
        mViewModel?.loadArticleTop(false)
    }
}