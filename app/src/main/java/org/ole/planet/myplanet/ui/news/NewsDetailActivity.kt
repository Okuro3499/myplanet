package org.ole.planet.myplanet.ui.news

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_news_detail.*
import kotlinx.android.synthetic.main.content_news_detail.*
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.base.BaseActivity
import org.ole.planet.myplanet.datamanager.DatabaseService
import org.ole.planet.myplanet.model.RealmMyLibrary
import org.ole.planet.myplanet.model.RealmNews
import org.ole.planet.myplanet.model.RealmNewsLog
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.utilities.NetworkUtils
import org.ole.planet.myplanet.utilities.Utilities
import org.w3c.dom.UserDataHandler
import java.io.File
import java.util.*

class NewsDetailActivity : BaseActivity() {
    var news: RealmNews? = null
    lateinit var realm: Realm
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_detail)
        setSupportActionBar(toolbar)
        initActionBar()
        realm = DatabaseService(this).realmInstance
        var id = intent.getStringExtra("newsId")
        news = realm.where(RealmNews::class.java).equalTo("id", id).findFirst()
        if (news == null) {
            Utilities.toast(this, "New not available")
            finish()
            return
        }
        var user = UserProfileDbHandler(this).userModel
        var userId = user.id
        realm.executeTransactionAsync {
            var newsLog: RealmNewsLog = it.createObject(RealmNewsLog::class.java, UUID.randomUUID().toString())
            newsLog.androidId = NetworkUtils.getMacAddr()
            newsLog.type = "news"
            newsLog.time = Date().time
            if (user != null)
                newsLog.userId =userId
        }
        initViews()
    }

    private fun initViews() {
        title = news?.userName
        tv_detail.text = news?.message
        val imageUrl = news?.imageUrl
        if (TextUtils.isEmpty(imageUrl)) {
            loadImage()
        } else {
            try {
                img.visibility = View.VISIBLE
                Glide.with(this)
                        .load(File(imageUrl))
                        .into(img)
            } catch (e: Exception) {
                loadImage()
            }
        }
    }

    private fun loadImage() {
        if (news?.images != null && news?.images!!.size > 0) {
            val library = realm.where(RealmMyLibrary::class.java).equalTo("_id", news!!.images[0]).findFirst()
            if (library != null) {
                Glide.with(this)
                        .load(File(Utilities.SD_PATH, library!!.getId() + "/" + library!!.getResourceLocalAddress()))
                        .into(img)
                img.visibility = View.VISIBLE
                return
            }
        }
        img.visibility = View.GONE
    }
}