package com.sungeo.bluetoothdemo;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class ArrayListAdapter extends BaseAdapter{
	private int mSelecteItem;
	private ArrayList<String> mSecEleName = new ArrayList<String>(0);
	private ArrayList<String> mElementName = new ArrayList<String>(0);
	private Context mContext;
	public ArrayListAdapter(Context context, ArrayList<String> objects, ArrayList<String> secObj) {
		mContext = context;
		mElementName = objects;
		mSecEleName = secObj;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) { 
		View view=convertView;  
		if(view==null){  
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
			view=inflater.inflate(R.layout.array_list_item, null);  
		}  

		if (getmElementName() == null) {
			return null;
		}

		TextView indexText = (TextView) view.findViewById(R.id.index_text);
		indexText.setText((position + 1) + ". ");
		
		TextView elementText = (TextView) view.findViewById(R.id.element_text);
		elementText.setText(mElementName.get(position));
		
		TextView secEleText = (TextView) view.findViewById(R.id.album_text);
		secEleText.setText(mSecEleName.get(position));

		RelativeLayout layout = (RelativeLayout) view.findViewById(R.id.array_item_layout);
		if (mSelecteItem == position) {
			layout.setBackgroundColor(0xffffcc00);
		} else {
			layout.setBackgroundColor(Color.TRANSPARENT);
		}

		return view;
	}

	public void setSelected(int selected) {
		this.mSelecteItem = selected;
	}
	public int getSelected() {
		return mSelecteItem;
	}

	public void setmElementName(ArrayList<String> mElementName) {
		this.mElementName = mElementName;
	}

	public ArrayList<String> getmElementName() {
		return mElementName;
	}

	@Override
	public int getCount() {
		if (mElementName == null) {
			return 0;
		}
		return mElementName.size();
	}

	@Override
	public Object getItem(int position) {
		if (mElementName == null) {
			return null;
		}
		
		return mElementName.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

    public void setmSecEleName(ArrayList<String> mSecEleName) {
        this.mSecEleName = mSecEleName;
    }

    public ArrayList<String> getmSecEleName() {
        return mSecEleName;
    }
}
