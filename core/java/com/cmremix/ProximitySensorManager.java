/*
 * Copyright (C) 2015 The MoKee OpenSource Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cmremix;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemProperties;

public class ProximitySensorManager {

    public interface ProximitySensorListener {
        public void onPickup();
    }

    private final ProximitySensorEventListener mProximitySensorListener;

    private boolean mManagerEnabled;

    private class ProximitySensorEventListener implements SensorEventListener {
        private int SensorOrientationY = 0;
        private int SensorProximity = 0;
        private boolean initProx = true;
        private boolean proxChanged = false;
        private float[] mGravity;
        private float[] mGeomagnetic;

        private final SensorManager mSensorManager;
        private final Sensor mProximitySensor;
        private final Sensor mAcceleroMeter;
        private final Sensor mMagneticSensor;
        private final ProximitySensorListener mListener;
        private final float mMaxProximityValue;

        public ProximitySensorEventListener(SensorManager sensorManager, Sensor proximitySensor,
                Sensor acceleroMeter,
                Sensor magneticSensor, ProximitySensorListener listener) {
            mSensorManager = sensorManager;
            mProximitySensor = proximitySensor;
            mAcceleroMeter = acceleroMeter;
            mMagneticSensor = magneticSensor;
            mMaxProximityValue = proximitySensor.getMaximumRange();
            mListener = listener;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float value = event.values[0];
            if (event.sensor.equals(mProximitySensor)) {
                int currentProx = (int) value;
                if (initProx) {
                    SensorProximity = currentProx;
                    initProx = false;
                } else {
                    if (SensorProximity > 0 && currentProx <= mMaxProximityValue) {
                        proxChanged = true;
                    }
                }
                SensorProximity = currentProx;
            } else if (event.sensor.equals(mAcceleroMeter)) {
                mGravity = event.values;
            } else if (event.sensor.equals(mMagneticSensor)) {
                mGeomagnetic = event.values;
            }
            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float orientation[] = new float[5];
                    SensorManager.getOrientation(R, orientation);
                    SensorOrientationY = (int) (orientation[1] * 180f / Math.PI);
                }
            }
            if (rightOrientation(SensorOrientationY) && SensorProximity <= mMaxProximityValue
                    && proxChanged) {
                mListener.onPickup();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        private boolean rightOrientation(int orientation) {
            if (orientation < -50 && orientation > -130) {
                return true;
            } else {
                return false;
            }
        }

        public void register() {
            SensorOrientationY = 0;
            SensorProximity = 0;
            initProx = true;
            proxChanged = false;
            registerSensorListener(mProximitySensor);
            registerSensorListener(mAcceleroMeter);
            registerSensorListener(mMagneticSensor);
        }

        private void registerSensorListener(Sensor sensor) {
            if (sensor != null) {
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
            }
        }

        private void unregisterSensorListener(Sensor sensor) {
            if (sensor != null) {
                mSensorManager.unregisterListener(this, sensor);
            }
        }

        public void unregister() {
            unregisterSensorListener(mProximitySensor);
            unregisterSensorListener(mAcceleroMeter);
            unregisterSensorListener(mMagneticSensor);
        }
    }

    public ProximitySensorManager(Context context, ProximitySensorListener listener) {
        SensorManager sensorManager =
                (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        String trueVersion = SystemProperties.get("ro.modversion");
        Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        Sensor acceleroMeter = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (proximitySensor == null && acceleroMeter == null && magneticSensor == null || !trueVersion.startsWith("MK")) {
            mProximitySensorListener = null;
        } else {
            mProximitySensorListener =
                    new ProximitySensorEventListener(sensorManager, proximitySensor,
                            acceleroMeter, magneticSensor, listener);
        }
    }

    public void enable() {
        if (mProximitySensorListener != null && !mManagerEnabled) {
            mProximitySensorListener.register();
            mManagerEnabled = true;
        }
    }

    public void disable() {
        if (mProximitySensorListener != null && mManagerEnabled) {
            mProximitySensorListener.unregister();
            mManagerEnabled = false;
        }
    }
}
