package com.lanjian.bluetoothapplication.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * @author lanjian
 * creat at $date$
 * description
 */
public abstract class MyBaseAdapter<T> extends BaseAdapter {

    public List<T> list;
    public MyBaseAdapter(List<T> t) {
        list = t;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = myGetView(position,convertView,parent);
        return convertView;
    }

    abstract View myGetView(int position, View convertView, ViewGroup parent);

    public void addData(T t){
        list.add(t);
        notifyDataSetChanged();
    }
}
