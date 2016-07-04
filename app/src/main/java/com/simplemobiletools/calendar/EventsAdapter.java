package com.simplemobiletools.calendar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.simplemobiletools.calendar.models.Event;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EventsAdapter extends BaseAdapter {
    private final List<Event> mEvents;
    private final LayoutInflater mInflater;

    public EventsAdapter(Context context, List<Event> events) {
        mEvents = events;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.event_item, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final Event event = mEvents.get(position);
        viewHolder.eventTitle.setText(event.getTitle());
        viewHolder.eventStart.setText(Formatter.getTime(event.getStartTS()));
        viewHolder.eventEnd.setText(Formatter.getTime(event.getEndTS()));

        return convertView;
    }

    @Override
    public int getCount() {
        return mEvents.size();
    }

    @Override
    public Object getItem(int position) {
        return mEvents.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    static class ViewHolder {
        @BindView(R.id.event_item_title) TextView eventTitle;
        @BindView(R.id.event_item_start) TextView eventStart;
        @BindView(R.id.event_item_end) TextView eventEnd;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
