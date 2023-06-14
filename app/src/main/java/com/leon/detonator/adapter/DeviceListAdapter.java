package com.leon.detonator.adapter;

import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.leon.detonator.R;
import com.leon.detonator.bluetooth.BluetoothBean;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends BaseAdapter {
    private final List<BluetoothBean> list;
    private final LayoutInflater inflater;

    public DeviceListAdapter(Context context, List<BluetoothBean> list) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<BluetoothBean> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        BluetoothBean bean = list.get(i);
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view = inflater.inflate(R.layout.layout_item_device, viewGroup, false);
            viewHolder.rbSelected = view.findViewById(R.id.rb_selected);
            viewHolder.ivType = view.findViewById(R.id.iv_type);
            viewHolder.tvName = view.findViewById(R.id.tv_name);
            viewHolder.tvConnected = view.findViewById(R.id.tv_connected);
            view.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) view.getTag();
        viewHolder.rbSelected.setChecked(bean.isSelected());
        viewHolder.tvName.setText(bean.getName() == null || bean.getName().isEmpty() ? bean.getAddress() : bean.getName());
        if (bean.isConnected())
            viewHolder.tvConnected.setText(R.string.device_paired);
        else
            viewHolder.tvConnected.setText("");
        int deviceType = bean.getDeviceType();
        int btIcon = R.mipmap.ic_bluetooth_default_device;
        switch (deviceType) {
            case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:   //耳麦
                btIcon = R.mipmap.ic_bluetooth_handfree;
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:   //耳机
                btIcon = R.mipmap.ic_bluetooth_headphone;
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE:   //麦克风
                btIcon = R.mipmap.ic_bluetooth_microphone;
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:   //车载
                btIcon = R.mipmap.ic_bluetooth_car;
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER: //音箱
                btIcon = R.mipmap.ic_bluetooth_speaker;
                break;
            default:
                if (deviceType >= 0x0100 && deviceType < 0x0200) { //计算机设备
                    btIcon = R.mipmap.ic_bluetooth_computer;
                } else if (deviceType >= 0x0200 && deviceType < 0x0300) { //电话设备
                    btIcon = R.mipmap.ic_bluetooth_pad;
                } else if (deviceType >= 0x0300 && deviceType < 0x0400) { //网络设备
                    btIcon = R.mipmap.ic_bluetooth_network;
                } else if (deviceType >= 0x0400 && deviceType < 0x0500) { //音频视频设备
                    btIcon = R.mipmap.ic_bluetooth_audio;
                } else if (deviceType == 0x0580) { //鼠标
                    btIcon = R.mipmap.ic_bluetooth_mouse;
                } else if (deviceType >= 0x0500 && deviceType < 0x0600) { //键盘
                    btIcon = R.mipmap.ic_bluetooth_keyboard;
                } else if (deviceType >= 0x0700 && deviceType < 0x0800) { //穿戴设备
                    btIcon = R.mipmap.ic_bluetooth_wearable;
                } else if (deviceType >= 0x0800 && deviceType < 0x0900) { //机器人设备
                    btIcon = R.mipmap.ic_bluetooth_joy;
                } else if (deviceType >= 0x0900 && deviceType < 0x0A00) { //健康器材
                    btIcon = R.mipmap.ic_bluetooth_health;
                }
                break;
        }
        viewHolder.ivType.setImageResource(btIcon);
        return view;
    }

    private static class ViewHolder {
        ImageView ivType;
        TextView tvName;
        TextView tvConnected;
        RadioButton rbSelected;
    }
}
