package com.ziqi.wanandroid.ui.home

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.youth.banner.config.IndicatorConfig
import com.youth.banner.indicator.CircleIndicator
import com.youth.banner.listener.OnPageChangeListener
import com.youth.banner.transformer.DepthPageTransformer
import com.youth.banner.util.BannerUtils
import com.ziqi.baselibrary.common.WebInfo
import com.ziqi.baselibrary.view.status.ZStatusViewBuilder
import com.ziqi.baselibrary.view.viewpager2.BaseFragmentStateAdapter
import com.ziqi.wanandroid.R
import com.ziqi.wanandroid.bean.Banner
import com.ziqi.wanandroid.databinding.FragmentHomeBinding
import com.ziqi.wanandroid.ui.common.BaseFragment
import com.ziqi.wanandroid.ui.recentblog.RecentBlogFragment
import com.ziqi.wanandroid.ui.recentproject.RecentProjectFragment
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
class HomeFragment : BaseFragment<HomeViewModel, Parcelable, FragmentHomeBinding>() {

    private var mTabLayoutMediator: TabLayoutMediator? = null

    companion object {
        @JvmStatic
        fun newInstance(bundle: Bundle?): HomeFragment {
            var mWBaseFragment = HomeFragment()
            mWBaseFragment.arguments = bundle
            return mWBaseFragment
        }
    }

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
        initView()
        dealViewModel()
        onRefresh()
    }

    override fun initView() {
        mViewDataBinding?.homeHeader?.banner?.apply {
            setIndicator(CircleIndicator(context))
            setIndicatorSelectedColorRes(R.color.colorPrimary)
            setIndicatorNormalColorRes(R.color.color_999999)
            setIndicatorGravity(IndicatorConfig.Direction.LEFT)
            setIndicatorSpace(BannerUtils.dp2px(20f).toInt())
            setIndicatorMargins(IndicatorConfig.Margins(BannerUtils.dp2px(10f).toInt()))
            setIndicatorWidth(10, 20)
            setPageTransformer(DepthPageTransformer())
            setOnBannerListener { data, _ ->
                activity?.let {
                    val webInfo = WebInfo()
                    webInfo.url = (data as Banner).url
                    StartUtil.startWebFragment(it, this@HomeFragment, -1, webInfo)
                }
            }
        }
        var mAdapter = object : BaseFragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 2
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> RecentBlogFragment.newInstance()
                    else -> RecentProjectFragment.newInstance()
                }
            }
        }
        mViewDataBinding?.viewPager2?.apply {
            adapter = mAdapter
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    //.,.
                }
            })
        }
        mViewDataBinding?.viewPager2?.isUserInputEnabled = false; //true:滑动，false：禁止滑动

        mTabLayoutMediator = TabLayoutMediator(
            mViewDataBinding?.tabLayout!!,
            mViewDataBinding?.viewPager2!!,
            TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                when (position) {
                    0 -> tab.text = "最新博客"
                    else -> tab.text = "最新项目"
                }
            })
        mTabLayoutMediator?.attach()
    }

    override fun dealViewModel() {
        mViewModel?.mBanner?.observe(viewLifecycleOwner, Observer {
            mViewDataBinding?.homeHeader?.banner?.apply {
                adapter = ImageAdapter(it)
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
                            val banner = it[position]
                            mViewDataBinding?.homeHeader?.title?.text = banner.title
                        }
                    }
                })
                start()
            }
        })
    }


    override fun onResume() {
        super.onResume()
        mViewDataBinding?.homeHeader?.banner?.start()
    }

    override fun onPause() {
        super.onPause()
        mViewDataBinding?.homeHeader?.banner?.stop()
    }

    override fun onRefresh() {
        mViewModel?.loadBanner()
    }

    override fun onDestroy() {
        super.onDestroy()
        mTabLayoutMediator?.detach()
    }
}