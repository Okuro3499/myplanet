package org.ole.planet.myplanet.ui.team

import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.databinding.FragmentPlanBinding
import org.ole.planet.myplanet.utilities.TimeUtils.formatDate
import org.ole.planet.myplanet.utilities.Utilities

class PlanFragment : BaseTeamFragment() {
    private lateinit var fragmentPlanBinding: FragmentPlanBinding
    private var missionText: String? = null
    private var servicesText: String? = null
    private var rulesText = ""
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentPlanBinding = FragmentPlanBinding.inflate(inflater, container, false)
        return fragmentPlanBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (team != null) {
            Utilities.log(team!!.type)
            Utilities.log(team!!.services)
            Utilities.log(team!!.rules)
            if (TextUtils.equals(team!!.type, "enterprise")) {
                missionText = if (team!!.description!!.trim { it <= ' ' }.isEmpty()) "" else "<b>" + getString(R.string.entMission) + "</b><br/>" + team!!.description + "<br/><br/>"
                servicesText = if (team!!.services!!.trim { it <= ' ' }.isEmpty()) "" else "<b>" + getString(R.string.entServices) + "</b><br/>" + team!!.services + "<br/><br/>"
                rulesText = if (team!!.rules!!.trim { it <= ' ' }.isEmpty()) "" else "<b>" + getString(R.string.entRules) + "</b><br/>" + team!!.rules
                fragmentPlanBinding.tvDescription.text = Html.fromHtml(missionText + servicesText + rulesText)
                if (fragmentPlanBinding.tvDescription.text.toString().isEmpty())
                    fragmentPlanBinding.tvDescription.text = Html.fromHtml("<br/>" + getString(R.string.entEmptyDescription) + "<br/>")
            } else {
                fragmentPlanBinding.tvDescription.text = team!!.description
            }
            fragmentPlanBinding.tvDate.text = "${getString(R.string.created_on)} ${formatDate(team!!.createdDate)}"
        }
    }
}