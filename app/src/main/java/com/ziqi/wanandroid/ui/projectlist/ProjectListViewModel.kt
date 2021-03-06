package com.ziqi.wanandroid.ui.projectlist

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ziqi.wanandroid.commonlibrary.bean.ListProject
import com.ziqi.wanandroid.commonlibrary.net.NetRepository
import com.ziqi.wanandroid.commonlibrary.ui.common.BaseViewModel
import kotlinx.coroutines.async

/**
 * Copyright (C), 2018-2020
 * Author: ziqimo
 * Date: 2020/5/2 8:01 PM
 * Description:
 * History:
 * <author> <time> <version> <desc>
 * 作者姓名 修改时间 版本号 描述
 */
class ProjectListViewModel(ctx: Application) : BaseViewModel(ctx) {

    private val TAG: String = ProjectListViewModel::class.java.simpleName

    private var _mListProject: MutableLiveData<ListProject> = MutableLiveData()
    val mListProject: LiveData<ListProject>
        get() = _mListProject

    fun loadListProject(pos: Int, cid: Int) = asyncExt({
        _mListProject.value = async { NetRepository.project(pos, cid).preProcessData() }.await()
        zContentView()
        if (pos == 0) {
            zRefresh(true)
        } else {
            zLoadMore(true)
        }
    }, {
        zToast(errorInfo(it))
        if (pos == 0) {
            if (_mListProject.value == null) {
                zErrorView()
            }
            zRefresh(false)
        } else {
            zLoadMore(false)
        }
    })
}