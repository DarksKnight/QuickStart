package com.sea.quickstart.activity

import android.Manifest
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.permissionx.guolindev.PermissionCollection
import com.permissionx.guolindev.PermissionX
import com.sea.quickstart.R
import com.sea.quickstart.service.TouchHelperService


class PermissionActivity : FragmentActivity() {

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
            Permission("读写权限", false),
            Permission("允许使用悬浮窗", false),
            Permission("辅助功能", false),
            Permission("电池优化", false)
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
                if (permissionList[holder.adapterPosition].isGrant) {
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
            if (permissionList[position].isGrant) {
                holder.tvDesc.text = "已授权"
            } else {
                holder.tvDesc.text = "未授权"
            }
        }

        override fun getItemCount() = permissionList.size

        fun loadPermissionsState() {
            //判断sd卡读写权限
            permissionList[0].isGrant = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            //判断悬浮窗权限
            permissionList[1].isGrant = canDrawOverlays(activity)
            //判断辅助功能权限
            permissionList[2].isGrant = isAccessibilitySettingsOn(activity)
        }

        fun canDrawOverlays(context: Context): Boolean {
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
    }

    class Permission constructor(title: String, isGrant: Boolean) {
        var title: String = title
        var isGrant: Boolean = isGrant
    }
}