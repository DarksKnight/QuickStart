package com.sea.quickstart.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.permissionx.guolindev.PermissionCollection
import com.permissionx.guolindev.PermissionX
import com.sea.quickstart.R


class PermissionActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        val rvList = findViewById<RecyclerView>(R.id.rv_list)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        rvList.layoutManager = layoutManager
        rvList.adapter = ItemAdapter(this)
    }

    override fun onResume() {
        super.onResume()
    }

    class ItemAdapter(activity: FragmentActivity) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

        private val activity: FragmentActivity = activity
        private lateinit var permission: PermissionCollection

        private val permissionList: List<Permission> = listOf(
            Permission("读写权限", ""),
            Permission("允许使用悬浮窗", ""),
            Permission("辅助功能", "")
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
                when (holder.adapterPosition) {
                    0 -> {
                        permission.permissions(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ).request(null)
                    }
                    1 -> {
                        if (!canDrawOverlays(activity)) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            intent.data = Uri.parse("package:" + activity.packageName)
                            activity.startActivityForResult(intent, 0)
                        }
                    }
                    2 -> {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        activity.startActivity(intent)
                    }
                }
            }
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvTitle.text = permissionList[position].title
            holder.tvDesc.text = permissionList[position].desc
        }

        override fun getItemCount() = permissionList.size

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
    }

    class Permission constructor(title: String, desc: String) {
        var title: String = title
        var desc: String = desc
    }
}