package com.sea.quickstart.activity

import android.Manifest
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.permissionx.guolindev.PermissionCollection
import com.permissionx.guolindev.PermissionX
import com.sea.quickstart.R
import com.sea.quickstart.helper.SpHelper
import com.sea.quickstart.service.TouchHelperService


class PermissionActivity : AppCompatActivity() {

    private lateinit var adapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        val rvList = findViewById<RecyclerView>(R.id.rv_list)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        rvList.layoutManager = layoutManager
        adapter = ItemAdapter(this)
        rvList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        adapter.loadPermissionsState()
        adapter.notifyDataSetChanged()
    }

    class ItemAdapter(activity: FragmentActivity) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

        private val activity: FragmentActivity = activity
        private lateinit var permission: PermissionCollection

        private val permissionList: List<Permission> = listOf(
            Permission("读写权限", "", Permission.State.DENIED),
            Permission("允许使用悬浮窗", "", Permission.State.DENIED),
            Permission("辅助功能", "", Permission.State.DENIED),
            Permission("电池优化"),
            Permission("隐藏最近任务列表"),
            Permission("自启动管理")
        )

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var rlContainer: RelativeLayout = itemView.findViewById(R.id.rl_container)
            var tvTitle: TextView = itemView.findViewById(R.id.tv_title)
            var tvDesc: TextView = itemView.findViewById(R.id.tv_desc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            permission = PermissionX.init(activity)
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_permission, parent, false)
            val holder = ViewHolder(view)
            holder.rlContainer.setOnClickListener {
                if (permissionList[holder.adapterPosition].state == Permission.State.GRANTED) {
                    return@setOnClickListener
                }
                when (holder.adapterPosition) {
                    0 -> {
                        permission.permissions(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ).request(null)
                    }
                    1 -> {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        intent.data = Uri.parse("package:" + activity.packageName)
                        activity.startActivityForResult(intent, 0)
                    }
                    2 -> {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        activity.startActivity(intent)
                    }
                    3 -> {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        intent.data = Uri.parse("package:" + activity.packageName)
                        activity.startActivity(intent)
                    }
                    4 -> {
                        val am =
                            activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        am?.let {
                            val tasks = it.appTasks
                            if (tasks.isNullOrEmpty()) {
                                return@let
                            }
                            val flag = SpHelper.get(activity, "excludeRecent")
                            if (flag == "1") {
                                tasks[0].setExcludeFromRecents(false)
                                SpHelper.put(activity, "excludeRecent", "0")
                            } else {
                                tasks[0].setExcludeFromRecents(true)
                                SpHelper.put(activity, "excludeRecent", "1")
                            }
                            loadPermissionsState()
                            notifyDataSetChanged()
                        }
                    }
                    5 -> {
                        jumpStartInterface(activity)
                    }
                }
            }
            loadPermissionsState()
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvTitle.text = permissionList[position].title
            if (position == 3) {
                return;
            }
            when (permissionList[position].state) {
                Permission.State.GRANTED -> holder.tvDesc.text = "已授权"
                Permission.State.DENIED -> holder.tvDesc.text = "未授权"
            }
            if (permissionList[position].desc.isNotEmpty()) {
                holder.tvDesc.text = permissionList[position].desc
            }
        }

        override fun getItemCount() = permissionList.size

        fun loadPermissionsState() {
            //判断sd卡读写权限
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                permissionList[0].state == Permission.State.GRANTED
            } else {
                permissionList[0].state == Permission.State.DENIED
            }
            //判断悬浮窗权限
            if (canDrawOverlays(activity)) {
                permissionList[1].state = Permission.State.GRANTED
            } else {
                permissionList[1].state == Permission.State.DENIED
            }
            //判断辅助功能权限
            if (isAccessibilitySettingsOn(activity)) {
                permissionList[2].state = Permission.State.GRANTED
            } else {
                permissionList[2].state == Permission.State.DENIED
            }
            //更新隐藏状态
            val flag = SpHelper.get(activity, "excludeRecent")
            if (flag == "1") {
                permissionList[4].desc = "已隐藏"
            } else {
                permissionList[4].desc = "未隐藏"
            }
        }

        private fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) true else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                Settings.canDrawOverlays(context)
            } else {
                if (Settings.canDrawOverlays(context)) return true
                try {
                    val mgr = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    val viewToAdd = View(context)
                    val params = WindowManager.LayoutParams(
                        0,
                        0,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSPARENT
                    )
                    viewToAdd.layoutParams = params
                    mgr.addView(viewToAdd, params)
                    mgr.removeView(viewToAdd)
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                false
            }
        }

        private fun isAccessibilitySettingsOn(mContext: Context): Boolean {
            var accessibilityEnabled = 0
            val service: String =
                mContext.packageName + "/" + TouchHelperService::class.java.canonicalName
            try {
                accessibilityEnabled = Settings.Secure.getInt(
                    mContext.applicationContext.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED
                )
            } catch (e: SettingNotFoundException) {
                e.printStackTrace()
            }
            val mStringColonSplitter = SimpleStringSplitter(':')
            if (accessibilityEnabled == 1) {
                val settingValue = Settings.Secure.getString(
                    mContext.applicationContext.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                if (settingValue != null) {
                    mStringColonSplitter.setString(settingValue)
                    while (mStringColonSplitter.hasNext()) {
                        val accessibilityService = mStringColonSplitter.next()
                        if (accessibilityService.equals(service, ignoreCase = true)) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        private fun getMobileType(): String {
            return Build.MANUFACTURER
        }

        private fun jumpStartInterface(context: Context) {
            var intent = Intent()
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                var componentName: ComponentName? = null
                if (getMobileType() == "Xiaomi") { // 红米Note4测试通过
                    componentName = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                } else if (getMobileType() == "Letv") { // 乐视2测试通过
                    intent.action = "com.letv.android.permissionautoboot"
                } else if (getMobileType() == "samsung") { // 三星Note5测试通过
                    componentName = ComponentName(
                        "com.samsung.android.sm_cn",
                        "com.samsung.android.sm.ui.ram.AutoRunActivity"
                    )
                } else if (getMobileType() == "HUAWEI") { // 华为测试通过
                    componentName =
                        ComponentName.unflattenFromString("com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity") //跳自启动管理
                    //SettingOverlayView.show(context);
                } else if (getMobileType() == "vivo") { // VIVO测试通过
                    componentName =
                        ComponentName.unflattenFromString("com.iqoo.secure/.safeguard.PurviewTabActivity")
                } else if (getMobileType() == "Meizu") { //万恶的魅族
                    // 通过测试，发现魅族是真恶心，也是够了，之前版本还能查看到关于设置自启动这一界面，系统更新之后，完全找不到了，心里默默Fuck！
                    // 针对魅族，我们只能通过魅族内置手机管家去设置自启动，所以我在这里直接跳转到魅族内置手机管家界面，具体结果请看图
                    componentName =
                        ComponentName.unflattenFromString("com.meizu.safe/.permission.PermissionMainActivity")
                } else if (getMobileType() == "OPPO") { // OPPO R8205测试通过
                    componentName =
                        ComponentName.unflattenFromString("com.oppo.safe/.permission.startup.StartupAppListActivity")
                } else if (getMobileType() == "ulong") { // 360手机 未测试
                    componentName = ComponentName(
                        "com.yulong.android.coolsafe",
                        ".ui.activity.autorun.AutoRunListActivity"
                    )
                } else {
                    // 以上只是市面上主流机型，由于公司你懂的，所以很不容易才凑齐以上设备
                    // 针对于其他设备，我们只能调整当前系统app查看详情界面
                    // 在此根据用户手机当前版本跳转系统设置界面
                    if (Build.VERSION.SDK_INT >= 9) {
                        intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                        intent.data = Uri.fromParts("package", context.packageName, null)
                    } else if (Build.VERSION.SDK_INT <= 8) {
                        intent.action = Intent.ACTION_VIEW
                        intent.setClassName(
                            "com.android.settings",
                            "com.android.settings.InstalledAppDetails"
                        )
                        intent.putExtra(
                            "com.android.settings.ApplicationPkgName",
                            context.packageName
                        )
                    }
                }
                intent.component = componentName
                context.startActivity(intent)
            } catch (e: java.lang.Exception) { //抛出异常就直接打开设置页面
                intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)
            }
        }
    }

    class Permission constructor(title: String, desc: String = "", state: State = State.DEFAULT) {
        var title: String = title
        var desc: String = desc
        var state: State = state

        enum class State {
            DEFAULT,
            GRANTED,
            DENIED
        }
    }
}