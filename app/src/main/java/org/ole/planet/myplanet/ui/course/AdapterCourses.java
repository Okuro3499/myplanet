package org.ole.planet.myplanet.ui.course;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;
import com.google.gson.JsonObject;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.callback.OnCourseItemSelected;
import org.ole.planet.myplanet.callback.OnHomeItemClickListener;
import org.ole.planet.myplanet.callback.OnRatingChangeListener;
import org.ole.planet.myplanet.databinding.RowCourseBinding;
import org.ole.planet.myplanet.model.RealmMyCourse;
import org.ole.planet.myplanet.model.RealmTag;
import org.ole.planet.myplanet.utilities.JsonUtils;
import org.ole.planet.myplanet.utilities.TimeUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import fisk.chipcloud.ChipCloud;
import fisk.chipcloud.ChipCloudConfig;
import io.noties.markwon.Markwon;
import io.noties.markwon.movement.MovementMethodPlugin;
import io.realm.Realm;

public class AdapterCourses extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private RowCourseBinding rowCourseBinding;
    private Context context;
    private List<RealmMyCourse> courseList;
    private List<RealmMyCourse> selectedItems;
    private OnCourseItemSelected listener;
    private OnHomeItemClickListener homeItemClickListener;
    private HashMap<String, JsonObject> map;
    private HashMap<String, JsonObject> progressMap;
    private OnRatingChangeListener ratingChangeListener;
    private Realm mRealm;
    private ChipCloudConfig config;
    private Markwon markwon;
    private boolean isAscending = true;
    private boolean isTitleAscending = true;
    private boolean areAllSelected = true;

    public AdapterCourses(Context context, List<RealmMyCourse> courseList, HashMap<String, JsonObject> map) {
        this.map = map;
        this.context = context;
        this.courseList = courseList;
        markwon = Markwon.builder(context)
                .usePlugin(MovementMethodPlugin.none())
                .build();
        this.selectedItems = new ArrayList<>();
        if (context instanceof OnHomeItemClickListener) {
            homeItemClickListener = (OnHomeItemClickListener) context;
        }
        config = Utilities.getCloudConfig().selectMode(ChipCloud.SelectMode.single);
    }

    public static void showRating(JsonObject object, TextView average, TextView ratingCount, AppCompatRatingBar ratingBar) {
        average.setText(String.format("%.2f", object.get("averageRating").getAsFloat()));
        ratingCount.setText(object.get("total").getAsInt() + " total");
        if (object.has("ratingByUser")) ratingBar.setRating(object.get("ratingByUser").getAsInt());
        else ratingBar.setRating(0);
    }

    public void setmRealm(Realm mRealm) {
        this.mRealm = mRealm;
    }

    public void setRatingChangeListener(OnRatingChangeListener ratingChangeListener) {
        this.ratingChangeListener = ratingChangeListener;
    }

    public List<RealmMyCourse> getCourseList() {
        return courseList;
    }

    public void setCourseList(List<RealmMyCourse> courseList) {
        this.courseList = courseList;
        sortCourseList();
        sortCourseListByTitle();
        notifyDataSetChanged();
    }

    private void sortCourseListByTitle() {
        Collections.sort(courseList, (course1, course2) -> {
            if (isTitleAscending) {
                return course1.courseTitle.compareToIgnoreCase(course2.courseTitle);
            } else {
                return course2.courseTitle.compareToIgnoreCase(course1.courseTitle);
            }
        });
    }

    private void sortCourseList() {
        Collections.sort(courseList, new Comparator<RealmMyCourse>() {
            @Override
            public int compare(RealmMyCourse course1, RealmMyCourse course2) {
                if (isAscending) {
                    return course1.createdDate.compareTo(course2.createdDate);
                } else {
                    return course2.createdDate.compareTo(course1.createdDate);
                }
            }
        });
    }

    public void toggleTitleSortOrder() {
        isTitleAscending = !isTitleAscending;
        sortCourseListByTitle();
        notifyDataSetChanged();
    }

    public void toggleSortOrder() {
        isAscending = !isAscending;
        sortCourseList();
        notifyDataSetChanged();
    }

    public void setProgressMap(HashMap<String, JsonObject> progressMap) {
        this.progressMap = progressMap;
    }

    public void setListener(OnCourseItemSelected listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        rowCourseBinding = RowCourseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHoldercourse(rowCourseBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof ViewHoldercourse) {
            ViewHoldercourse viewHolder = (ViewHoldercourse) holder;
            viewHolder.bind(position);
            viewHolder.rowCourseBinding.title.setText(courseList.get(position).courseTitle);
            viewHolder.rowCourseBinding.description.setText(courseList.get(position).description);
            markwon.setMarkdown(viewHolder.rowCourseBinding.description, courseList.get(position).description);
            setTextViewContent(viewHolder.rowCourseBinding.gradLevel, courseList.get(position).gradeLevel, viewHolder.rowCourseBinding.gradLevel, context.getString(R.string.grade_level_colon));
            setTextViewContent(viewHolder.rowCourseBinding.subjectLevel, courseList.get(position).subjectLevel, viewHolder.rowCourseBinding.subjectLevel, context.getString(R.string.subject_level_colon));
            viewHolder.rowCourseBinding.checkbox.setChecked(selectedItems.contains(courseList.get(position)));
            viewHolder.rowCourseBinding.courseProgress.setMax(courseList.get(position).getnumberOfSteps());
            displayTagCloud(viewHolder.rowCourseBinding.flexboxDrawable, position);
            try {
                viewHolder.rowCourseBinding.tvDate.setText(TimeUtils.formatDate(Long.parseLong(courseList.get(position).createdDate.trim()), "MMM dd, yyyy"));
            } catch (Exception e) {

            }
            viewHolder.rowCourseBinding.ratingBar.setOnTouchListener((v1, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP)
                    homeItemClickListener.showRatingDialog("course", courseList.get(position).courseId, courseList.get(position).courseTitle, ratingChangeListener);
                return true;
            });

            viewHolder.rowCourseBinding.checkbox.setOnClickListener((view) -> {
                Utilities.handleCheck(((CheckBox) view).isChecked(), position, (ArrayList) selectedItems, courseList);
                if (listener != null) listener.onSelectedListChange(selectedItems);
            });
            showProgressAndRating(position, holder);
        }
    }

    private void setTextViewContent(TextView textView, String content, View layout, String prefix) {
        if (content.isEmpty()) {
            layout.setVisibility(View.GONE);
        } else {
            textView.setText(prefix + content);
        }
    }

    public boolean areAllSelected(){
        if (selectedItems.size() != courseList.size()) {
            areAllSelected = false;
        } else {
            areAllSelected = true;
        }

        return areAllSelected;
    }

    public void selectAllItems(boolean selectAll) {
        if (selectAll) {
            selectedItems.clear();
            selectedItems.addAll(courseList);
        } else {
            selectedItems.clear();
        }

        notifyDataSetChanged();

        if (listener != null) {
            listener.onSelectedListChange(selectedItems);
        }
    }


    private void displayTagCloud(FlexboxLayout flexboxDrawable, int position) {
        flexboxDrawable.removeAllViews();
        final ChipCloud chipCloud = new ChipCloud(context, flexboxDrawable, config);
        List<RealmTag> tags = mRealm.where(RealmTag.class).equalTo("db", "courses").equalTo("linkId", courseList.get(position).id).findAll();
        showTags(tags, chipCloud);
    }

    private void showTags(List<RealmTag> tags, ChipCloud chipCloud) {
        for (RealmTag tag : tags) {
            RealmTag parent = mRealm.where(RealmTag.class).equalTo("id", tag.getTagId()).findFirst();
            showChip(chipCloud, parent);
        }
    }

    private void showChip(ChipCloud chipCloud, RealmTag parent) {
        chipCloud.addChip(((parent != null) ? parent.getName() : ""));
        chipCloud.setListener((i, b, b1) -> {
            if (b1 && listener != null) {
                listener.onTagClicked(parent);
            }
        });
    }

    private void showProgressAndRating(int position, RecyclerView.ViewHolder holder) {
        ViewHoldercourse viewHolder = (ViewHoldercourse) holder;
        showProgress(position, holder);
        if (map.containsKey(courseList.get(position).courseId)) {
            JsonObject object = map.get(courseList.get(position).courseId);
            showRating(object, viewHolder.rowCourseBinding.average, viewHolder.rowCourseBinding.timesRated, viewHolder.rowCourseBinding.ratingBar);
        } else {
            viewHolder.rowCourseBinding.ratingBar.setRating(0);
        }
    }

    private void showProgress(int position, RecyclerView.ViewHolder holder) {
        if (progressMap.containsKey(courseList.get(position).courseId)) {
            JsonObject ob = progressMap.get(courseList.get(position).courseId);
            rowCourseBinding.courseProgress.setMax(JsonUtils.getInt("max", ob));
            rowCourseBinding.courseProgress.setProgress(JsonUtils.getInt("current", ob));
            if (JsonUtils.getInt("current", ob) < JsonUtils.getInt("max", ob))
                rowCourseBinding.courseProgress.setSecondaryProgress(JsonUtils.getInt("current", ob) + 1);
            rowCourseBinding.courseProgress.setVisibility(View.VISIBLE);
        } else {
            rowCourseBinding.courseProgress.setVisibility(View.GONE);
        }
    }

    private void openCourse(RealmMyCourse realm_myCourses, int i) {
        if (homeItemClickListener != null) {
            Fragment f = new TakeCourseFragment();
            Bundle b = new Bundle();
            b.putString("id", realm_myCourses.courseId);
            b.putInt("position", i);
            f.setArguments(b);
            homeItemClickListener.openCallFragment(f);
        }
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    class ViewHoldercourse extends RecyclerView.ViewHolder {
        private final RowCourseBinding rowCourseBinding;
        private int adapterPosition;

        public ViewHoldercourse(RowCourseBinding rowCourseBinding) {
            super(rowCourseBinding.getRoot());
            this.rowCourseBinding = rowCourseBinding;
            itemView.setOnClickListener(v -> {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    openCourse(courseList.get(adapterPosition), 0);
                }
            });

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                rowCourseBinding.courseProgress.setScaleY(0.3f);
            }

            rowCourseBinding.courseProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && position < courseList.size()) {
                        if (progressMap.containsKey(courseList.get(getAdapterPosition()).courseId)) {
                            JsonObject ob = progressMap.get(courseList.get(getAdapterPosition()).courseId);
                            int current = JsonUtils.getInt("current", ob);
                            if (b && i <= current + 1) {
                                openCourse(courseList.get(getAdapterPosition()), seekBar.getProgress());
                            }
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }

        public void bind(int position) {
            adapterPosition = position; // Store the adapter position
        }
    }
}
