package com.lc.smartmnist

import com.mozhimen.basick.elemk.application.bases.BaseApplication
import com.youdao.sdk.app.YouDaoApplication

/**
 * @ClassName MainApplication
 * @Description TODO
 * @Author Mozhimen & Kolin Zhao
 * @Date 2023/5/11 11:23
 * @Version 1.0
 */
class MainApplication : BaseApplication() {
    override fun onCreate() {
        super.onCreate()

        YouDaoApplication.init(this, "1a9bbbed09908832")
    }
}