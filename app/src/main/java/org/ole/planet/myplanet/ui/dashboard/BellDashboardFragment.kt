package org.ole.planet.myplanet.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.realm.Case
import io.realm.Realm
import io.realm.RealmResults
import org.json.JSONException
import org.json.JSONObject
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.databinding.FragmentHomeBellBinding
import org.ole.planet.myplanet.model.RealmCertification
import org.ole.planet.myplanet.model.RealmCourseProgress
import org.ole.planet.myplanet.model.RealmCourseStep
import org.ole.planet.myplanet.model.RealmMyCourse
import org.ole.planet.myplanet.model.RealmMyCourse.Companion.getCourseByCourseId
import org.ole.planet.myplanet.model.RealmSubmission
import org.ole.planet.myplanet.ui.course.CourseFragment
import org.ole.planet.myplanet.ui.course.MyProgressFragment
import org.ole.planet.myplanet.ui.course.TakeCourseFragment
import org.ole.planet.myplanet.ui.feedback.FeedbackListFragment
import org.ole.planet.myplanet.ui.library.AddResourceFragment
import org.ole.planet.myplanet.ui.library.LibraryFragment
import org.ole.planet.myplanet.ui.mylife.LifeFragment
import org.ole.planet.myplanet.ui.submission.AdapterMySubmission
import org.ole.planet.myplanet.ui.submission.MySubmissionFragment
import org.ole.planet.myplanet.ui.survey.SurveyFragment
import org.ole.planet.myplanet.ui.team.TeamFragment
import org.ole.planet.myplanet.utilities.TimeUtils
import java.util.Date

class BellDashboardFragment : BaseDashboardFragment() {
    private lateinit var fragmentHomeBellBinding: FragmentHomeBellBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentHomeBellBinding = FragmentHomeBellBinding.inflate(inflater, container, false)

        val view = fragmentHomeBellBinding.root
        initView(view)
        declareElements()
        onLoaded(view)
        return fragmentHomeBellBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentHomeBellBinding.cardProfileBell.txtDate.text = TimeUtils.formatDate(Date().time)
        fragmentHomeBellBinding.cardProfileBell.txtCommunityName.text = model.planetCode
        (activity as DashboardActivity?)?.supportActionBar?.hide()
        fragmentHomeBellBinding.addResource.setOnClickListener {
            AddResourceFragment().show(childFragmentManager, getString(R.string.add_res))
        }
        showBadges()
        
        val noOfSurvey = RealmSubmission.getNoOfSurveySubmissionByUser(model.id, mRealm)
        if (noOfSurvey >= 1){
            val title: String = if (noOfSurvey > 1 ) {
                "surveys"
            } else{
                "survey"
            }
            val itemsQuery = mRealm.where(RealmSubmission::class.java).equalTo("userId", model.id)
                .equalTo("type", "survey").equalTo("status", "pending", Case.INSENSITIVE)
                .findAll()
            val courseTitles = itemsQuery.map { it.parent }
            val surveyNames = courseTitles.map { json ->
                try {
                    val jsonObject = json?.let { JSONObject(it) }
                    jsonObject?.getString("name")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            val alertDialog: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
            alertDialog.setTitle("You have $noOfSurvey $title to complete")
            val surveyNamesArray = surveyNames.filterNotNull().map { it as CharSequence }.toTypedArray()
            alertDialog.setItems(surveyNamesArray) { _, which ->
                val selectedSurvey = itemsQuery[which]?.id
                AdapterMySubmission.openSurvey(homeItemClickListener, selectedSurvey, true)
            }
            alertDialog.setPositiveButton("OK") { dialog, _ ->
                homeItemClickListener?.openCallFragment(MySubmissionFragment.newInstance("survey"))
                dialog.dismiss()
            }
            alertDialog.show()
        }
    }

    private fun showBadges() {
        fragmentHomeBellBinding.cardProfileBell.llBadges.removeAllViews()
        val courseCount = countCourseIds(mRealm)

        for ((index, entry) in courseCount.withIndex()) {
            val star = LayoutInflater.from(activity).inflate(R.layout.image_start, null) as ImageView
            val courseId = entry.keys.first()
            val count = entry.values.first()
            val steps = RealmCourseStep.getSteps(mRealm, courseId)
            if (count >= steps.size) {
                setColor(courseId, star)
                fragmentHomeBellBinding.cardProfileBell.llBadges.addView(star)
                star.setOnClickListener {
                    val course = getCourseByCourseId(courseId, mRealm)
                    star.contentDescription = "${getString(R.string.completed_course)} ${course?.courseTitle}"
                    openCourse(course, index)
                }
            }
        }
    }

    private fun openCourse(realmMyCourses: RealmMyCourse?, position: Int) {
        if (homeItemClickListener != null) {
            val f: Fragment = TakeCourseFragment()
            val b = Bundle()
            b.putString("id", realmMyCourses?.courseId)
            b.putInt("position", position)
            f.arguments = b
            homeItemClickListener?.openCallFragment(f)
        }
    }


    private fun countCourseIds(mRealm: Realm): List<Map<String, Long>> {
        val courseIdCounts: MutableMap<String, Long> = HashMap()
        val results: RealmResults<RealmCourseProgress> = mRealm.where(RealmCourseProgress::class.java).findAll()
        for (progress in results) {
            val courseId = progress.courseId
            if (courseId != null) {
                if (courseIdCounts.containsKey(courseId)) {
                    courseIdCounts[courseId] = courseIdCounts[courseId]!! + 1
                } else {
                    courseIdCounts[courseId] = 1
                }
            }
        }
        return courseIdCounts.map { mapOf(it.key to it.value) }
    }

    private fun setColor(courseId: String, star: ImageView) =
        if (RealmCertification.isCourseCertified(mRealm, courseId)) {
            star.setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        } else {
            star.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_blue_grey_300))
        }

    private fun declareElements() {
        fragmentHomeBellBinding.homeCardTeams.llHomeTeam.setOnClickListener { homeItemClickListener?.openCallFragment(TeamFragment()) }
        fragmentHomeBellBinding.homeCardLibrary.myLibraryImageButton.setOnClickListener { openHelperFragment(LibraryFragment()) }
        fragmentHomeBellBinding.homeCardCourses.myCoursesImageButton.setOnClickListener { openHelperFragment(CourseFragment()) }
        fragmentHomeBellBinding.fabMyProgress.setOnClickListener { openHelperFragment(MyProgressFragment()) }
        fragmentHomeBellBinding.fabMyActivity.setOnClickListener { openHelperFragment(MyActivityFragment()) }
        fragmentHomeBellBinding.fabSurvey.setOnClickListener { openHelperFragment(SurveyFragment()) }
        fragmentHomeBellBinding.cardProfileBell.fabFeedback.setOnClickListener { openHelperFragment(FeedbackListFragment()) }
        fragmentHomeBellBinding.homeCardMyLife.myLifeImageButton.setOnClickListener { homeItemClickListener?.openCallFragment(LifeFragment()) }
        fragmentHomeBellBinding.fabNotification.setOnClickListener { showNotificationFragment() }
    }

    private fun openHelperFragment(f: Fragment) {
        val b = Bundle()
        b.putBoolean("isMyCourseLib", true)
        f.arguments = b
        homeItemClickListener?.openCallFragment(f)
    }
}