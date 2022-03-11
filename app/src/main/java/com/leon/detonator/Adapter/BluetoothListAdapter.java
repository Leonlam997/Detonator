package com.leon.detonator.Adapter;

import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.leon.detonator.Bean.BluetoothListBean;
import com.leon.detonator.R;

import java.util.ArrayList;
import java.util.List;

public class BluetoothListAdapter extends BaseAdapter {
    private final List<BluetoothListBean> list;
    private final LayoutInflater inflater;

    private OnButtonClickListener onButtonClickListener;


    public BluetoothListAdapter(Context context, List<BluetoothListBean> list) {
        this.list = new ArrayList<>(list);
        inflater = LayoutInflater.from(context);
    }

    public void updateList(List<BluetoothListBean> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        int ret = 0;
        if (list != null) {
            ret = list.size();
        }
        return ret;
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
        BluetoothListBean bean = (BluetoothListBean) this.getItem(position);

        BluetoothListAdapter.ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();

            convertView = inflater.inflate(R.layout.layout_settings_btlist, parent, false);
            viewHolder.ivType = convertView.findViewById(R.id.ivType);
            viewHolder.tvName = convertView.findViewById(R.id.tvName);
            viewHolder.pbScan = convertView.findViewById(R.id.pbScan);
            viewHolder.tvRescan = convertView.findViewById(R.id.tvRescan);
            viewHolder.ivArrow = convertView.findViewById(R.id.ivArrow);
            viewHolder.cbSwitch = convertView.findViewById(R.id.cbBT);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (BluetoothListAdapter.ViewHolder) convertView.getTag();
        }

        viewHolder.pbScan.setVisibility(bean.isScanning() ? View.VISIBLE : View.INVISIBLE);
        viewHolder.tvRescan.setVisibility((!bean.isScanning() && bean.isRescanLine() && position != 2) || position == 1 ? View.VISIBLE : View.INVISIBLE);
        viewHolder.ivArrow.setVisibility((position == 1) ? View.VISIBLE : View.INVISIBLE);
        viewHolder.cbSwitch.setVisibility(position == 0 ? View.VISIBLE : View.INVISIBLE);
        viewHolder.ivType.setVisibility((position > 1 && !bean.isRescanLine()) ? View.VISIBLE : View.INVISIBLE);

        viewHolder.tvRescan.setTextColor(convertView.getContext().getColor(position == 1 ? R.color.colorHintText : R.color.colorRescanText));

        viewHolder.tvName.setText(bean.getBluetooth().getName() == null ? bean.getBluetooth().getAddress() : bean.getBluetooth().getName());

        switch (position) {
            case 0:
                convertView.setBackgroundColor(Color.WHITE);
                viewHolder.cbSwitch.setEnabled(!bean.isChangingStatus());
                viewHolder.cbSwitch.setChecked(bean.isEnabled());
                break;
            case 1:
                convertView.setBackgroundColor(Color.WHITE);
                viewHolder.tvRescan.setText(bean.getBluetooth().getAddress());
                break;
            default:
                if (bean.isRescanLine()) {
                    convertView.setBackgroundColor(convertView.getContext().getColor(R.color.colorCommonBackground));
                    viewHolder.tvRescan.setOnClickListener(v -> {
                        if (onButtonClickListener != null) {
                            onButtonClickListener.OnButtonClick(2);
                        }
                    });
                } else {
                    convertView.setBackgroundColor(Color.WHITE);
                    int deviceType = bean.getBluetooth().getDeviceType();
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

                    viewHolder.tvName.setTextColor(bean.getBluetooth().isConnected() ? Color.parseColor("#3891ff") : Color.BLACK);
                }
        }

        viewHolder.cbSwitch.setOnClickListener(v -> {
            if (onButtonClickListener != null) {
                onButtonClickListener.OnButtonClick(((CheckBox) v).isChecked() ? 1 : 0);
            }
        });

        return convertView;
    }

    public void setOnButtonClickListener(OnButtonClickListener listener) {
        onButtonClickListener = listener;
    }

    public interface OnButtonClickListener {
        void OnButtonClick(int which);
    }

    private static class ViewHolder {
        ImageView ivType;
        TextView tvName;
        ProgressBar pbScan;
        TextView tvRescan;
        ImageView ivArrow;
        CheckBox cbSwitch;
    }
}
