package org.ole.planet.myplanet.ui.team

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.applandeo.materialcalendarview.CalendarDay
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.listeners.OnCalendarDayClickListener
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.realm.Realm
import io.realm.RealmResults
import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.databinding.AddMeetupBinding
import org.ole.planet.myplanet.databinding.FragmentEnterpriseCalendarBinding
import org.ole.planet.myplanet.model.RealmMeetup
import org.ole.planet.myplanet.model.RealmNews
import org.ole.planet.myplanet.ui.mymeetup.AdapterMeetup
import org.ole.planet.myplanet.utilities.TimeUtils
import org.ole.planet.myplanet.utilities.Utilities

class TeamCalendarFragment : BaseTeamFragment() {
    private lateinit var fragmentEnterpriseCalendarBinding: FragmentEnterpriseCalendarBinding
    private val selectedDates: MutableList<Calendar> = mutableListOf()
    private lateinit var calendar: CalendarView
    private lateinit var list: List<Calendar>
    private lateinit var start: Calendar
    private lateinit var end: Calendar
    private lateinit var clickedCalendar: Calendar
    private lateinit var calendarEventsMap: MutableMap<CalendarDay, RealmMeetup>
    private lateinit var meetupList: RealmResults<RealmMeetup>
    private val eventDates: MutableList<Calendar> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentEnterpriseCalendarBinding = FragmentEnterpriseCalendarBinding.inflate(inflater, container, false)
        start = Calendar.getInstance()
        end = Calendar.getInstance()
        return fragmentEnterpriseCalendarBinding.root
    }

    fun String.isValidWebLink(): Boolean {
        val toCheck = when {
            startsWith("http://",  ignoreCase = true) || startsWith("https://", ignoreCase = true) -> this
            else -> "http://$this"
        }
        return try {
            val url = URL(toCheck)
            url.host.contains(".")
        } catch (e: MalformedURLException) {
            false
        }
    }

    private fun showMeetupAlert() {
        val addMeetupBinding = AddMeetupBinding.inflate(layoutInflater)
        setDatePickerListener(addMeetupBinding.tvStartDate, start, end)
        setDatePickerListener(addMeetupBinding.tvEndDate, end, null)
        setTimePicker(addMeetupBinding.tvStartTime)
        setTimePicker(addMeetupBinding.tvEndTime)
        if (!::clickedCalendar.isInitialized) {
            clickedCalendar = Calendar.getInstance()
        }
        val alertDialog = AlertDialog.Builder(requireActivity()).setView(addMeetupBinding.root).create()
        addMeetupBinding.btnSave.setOnClickListener {
            val title = "${addMeetupBinding.etTitle.text.trim()}"
            val link = "${addMeetupBinding.etLink.text.trim()}"
            val description = "${addMeetupBinding.etDescription.text.trim()}"
            val location = "${addMeetupBinding.etLocation.text.trim()}"
            if (title.isEmpty()) {
                Utilities.toast(activity, getString(R.string.title_is_required))
            } else if (description.isEmpty()) {
                Utilities.toast(activity, getString(R.string.description_is_required))
            } else if (!link.isValidWebLink() && link.isNotEmpty()) {
                Utilities.toast(activity, getString(R.string.invalid_url))
            } else {
                try {
                    if (!mRealm.isInTransaction) {
                        mRealm.beginTransaction()
                    }
                    val defaultPlaceholder = getString(R.string.click_here_to_pick_time)
                    val meetup = mRealm.createObject(RealmMeetup::class.java, "${UUID.randomUUID()}")
                    meetup.title = title
                    meetup.meetupLink = link
                    meetup.description = description
                    meetup.meetupLocation = location
                    meetup.creator = user?.name
                    meetup.startDate = start.timeInMillis
                    meetup.endDate = end.timeInMillis
                    if (addMeetupBinding.tvStartTime.text == defaultPlaceholder) {
                        meetup.startTime = ""
                    } else {
                        meetup.startTime = "${addMeetupBinding.tvStartTime.text}"
                    }
                    if (addMeetupBinding.tvEndTime.text == defaultPlaceholder) {
                        meetup.endTime = ""
                    } else {
                        meetup.endTime = "${addMeetupBinding.tvEndTime.text}"
                    }
                    meetup.createdDate = System.currentTimeMillis()
                    meetup.sourcePlanet = team?.teamPlanetCode
                    val jo = JsonObject()
                    jo.addProperty("type", "local")
                    jo.addProperty("planetCode", team?.teamPlanetCode)
                    meetup.sync = Gson().toJson(jo)
                    val rb = addMeetupBinding.rgRecuring.findViewById<RadioButton>(addMeetupBinding.rgRecuring.checkedRadioButtonId)
                    if (rb != null) {
                        meetup.recurring = "${rb.text}"
                    }
                    val ob = JsonObject()
                    ob.addProperty("teams", teamId)
                    meetup.link = Gson().toJson(ob)
                    meetup.teamId = teamId
                    mRealm.commitTransaction()
                    Utilities.toast(activity, getString(R.string.meetup_added))
                    alertDialog.dismiss()
                    refreshCalendarView()
                } catch (e: Exception) {
                    mRealm.cancelTransaction()
                    e.printStackTrace()
                    Utilities.toast(activity, getString(R.string.meetup_not_added))
                }
            }
        }

        addMeetupBinding.btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.setOnDismissListener {
            if (selectedDates.contains(clickedCalendar)) {
                selectedDates.remove(clickedCalendar)
                refreshCalendarView()
            }
        }
        alertDialog.show()
        alertDialog.window?.setBackgroundDrawableResource(R.color.card_bg)
    }

    private fun setDatePickerListener(view: TextView, date: Calendar?, endDate: Calendar?) {
        val initCal = date ?: Calendar.getInstance()
        if (date != null && endDate != null) {
            view.text = date.timeInMillis.let { it1 -> TimeUtils.formatDate(it1, "yyyy-MM-dd") }
        }
        view.setOnClickListener {
            DatePickerDialog(requireActivity(), { _, year, monthOfYear, dayOfMonth ->
                date?.set(Calendar.YEAR, year)
                date?.set(Calendar.MONTH, monthOfYear)
                date?.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                view.text = date?.timeInMillis?.let { it1 -> TimeUtils.formatDate(it1, "yyyy-MM-dd") }
            }, initCal.get(Calendar.YEAR),
                initCal.get(Calendar.MONTH),
                initCal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setTimePicker(time: TextView) {
        val c = Calendar.getInstance()
        time.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                activity, { _, hourOfDay, minute ->
                    time.text = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute) },
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true)
            timePickerDialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        setupCalendarClickListener()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list = mutableListOf()
        calendar = fragmentEnterpriseCalendarBinding.calendarView
        calendarEventsMap = mutableMapOf()
        setupCalendarClickListener()
    }

    private fun setupCalendarClickListener(){
        fragmentEnterpriseCalendarBinding.calendarView.setOnCalendarDayClickListener(object : OnCalendarDayClickListener {
            override fun onClick(calendarDay: CalendarDay) {
                meetupList = mRealm.where(RealmMeetup::class.java).equalTo("teamId", teamId).findAll()
                clickedCalendar = calendarDay.calendar
                val clickedDateInMillis = clickedCalendar.timeInMillis
                val clickedDate = Instant.ofEpochMilli(clickedDateInMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val markedDates = meetupList.mapNotNull { meetup ->
                    val meetupDate = Instant.ofEpochMilli(meetup.startDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    if (meetupDate == clickedDate) meetup else null
                }

                if (markedDates.isNotEmpty()) {
                    showMeetupDialog(markedDates)
                } else {
                    if(arguments?.getBoolean("fromLogin", false) != false || user?.id?.startsWith("guest") == true){
                        fragmentEnterpriseCalendarBinding.calendarView.selectedDates = eventDates
                    } else{
                        start = clickedCalendar.clone() as Calendar
                        end = clickedCalendar.clone() as Calendar
                        showMeetupAlert()
                    }
                }
                if (!selectedDates.contains(clickedCalendar)) {
                    selectedDates.add(clickedCalendar)
                } else {
                    selectedDates.remove(clickedCalendar)
                }
            }
        })
        refreshCalendarView()
    }

    override fun onNewsItemClick(news: RealmNews?) {}
    override fun clearImages() {
        imageList.clear()
        llImage?.removeAllViews()
    }

    private fun getCardViewHeight(context: Context): Int {
        val view = LayoutInflater.from(context).inflate(R.layout.item_meetup, null)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(Resources.getSystem().displayMetrics.widthPixels, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        return view.measuredHeight
    }

    private fun showMeetupDialog(meetupList: List<RealmMeetup>) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.meetup_dialog, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rvMeetups)
        val dialogTitle = dialogView.findViewById< TextView>(R.id.tvTitle)
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        dialogTitle.text = dateFormat.format(clickedCalendar.time)
        val extraHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics
        ).toInt()
        val cardHeight = getCardViewHeight(requireContext())
        recyclerView.layoutParams.height = cardHeight + extraHeight
        recyclerView.requestLayout()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = AdapterMeetup(meetupList)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        val btnAdd = dialogView.findViewById<Button>(R.id.btnadd)
        if (arguments?.getBoolean("fromLogin", false) != true) {
            btnAdd.visibility = View.VISIBLE
        } else {
            btnAdd.visibility = View.GONE
        }
        dialogView.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }
        btnAdd.setOnClickListener {
            if(arguments?.getBoolean("fromLogin", false) != true){
                start = clickedCalendar
                end = clickedCalendar
                showMeetupAlert()
            }
        }

        dialog.setOnDismissListener {
            eventDates.add(clickedCalendar)
            lifecycleScope.launch(Dispatchers.Main) {
                fragmentEnterpriseCalendarBinding.calendarView.selectedDates = emptyList()
                fragmentEnterpriseCalendarBinding.calendarView.selectedDates = eventDates.toList()
            }
            fragmentEnterpriseCalendarBinding.calendarView.selectedDates = eventDates
        }

        dialog.show()
    }

    private fun refreshCalendarView() {
        if (teamId.isEmpty()) {
            return
        }
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            var meetupList = mutableListOf<RealmMeetup>()
            val newDates = mutableListOf<Calendar>()
            val realm = Realm.getDefaultInstance()
            try {
                meetupList = realm.where(RealmMeetup::class.java).equalTo("teamId", teamId).findAll()
                val calendarInstance = Calendar.getInstance()

                for (meetup in meetupList) {
                    val startDateMillis = meetup.startDate
                    calendarInstance.timeInMillis = startDateMillis
                    newDates.add(calendarInstance.clone() as Calendar)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                realm.close()
            }
            withContext(Dispatchers.Main) {
                if (isAdded && activity != null) {
                    eventDates.clear()
                    eventDates.addAll(newDates)
                    fragmentEnterpriseCalendarBinding.calendarView.selectedDates = ArrayList(newDates)
                }
            }
        }
    }
}
