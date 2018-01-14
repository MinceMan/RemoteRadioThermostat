/*
 * Copyright 2016 Google Inc.
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

package com.google.android.things.contrib.driver.onewire;

import com.google.android.things.pio.I2cDevice;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.byteThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

public class Ds18b20Test {

    // Calibration values, sensor readings, and expected results for testing compensation functions,
    // taken from the BMP280 datasheet.
    // (https://ae-bst.resource.bosch.com/media/_tech/media/datasheets/BST-BMP280-DS001-18.pdf)
    private static final int[] TEMP_CALIBRATION = {27504, 26435, -1000};
    private static final int[] PRESSURE_CALIBRATION = {36477, -10685, 3024, 2855, 140, -7, 15500,
            -14600, 6000};
    private static final int[] HUM_CALIBRATION = {75, 363, 0, 315, 50, 30};
    private static final int RAW_HUMIDITY = 28437;
    private static final int RAW_TEMPERATURE = 519888;
    private static final int RAW_PRESSURE = 415148;

    private static final float EXPECTED_TEMPERATURE = 25.08f;
    private static final float EXPECTED_FINE_TEMPERATURE = 128422.0f;
    private static final float EXPECTED_PRESSURE = 1006.5327f;
    private static final float EXPECTED_HUMIDITY = 45.708218f;

    // Note: the datasheet points out that the calculated values can differ slightly because of
    // rounding. We'll check that the results are within a tolerance of 0.1%
    private static final float TOLERANCE = .001f;

    @Mock
    private I2cDevice mI2c;

    @Rule
    public MockitoRule mMokitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void testCompensateTemperature() {
        final float[] tempResults = Ds18b20.compensateTemperature(RAW_TEMPERATURE, TEMP_CALIBRATION);
        Assert.assertEquals(EXPECTED_TEMPERATURE, tempResults[0], EXPECTED_TEMPERATURE * TOLERANCE);
        Assert.assertEquals(EXPECTED_FINE_TEMPERATURE, tempResults[1],
                EXPECTED_FINE_TEMPERATURE * TOLERANCE);
    }

    @Test
    public void testCompensatePressure() {
        final float[] tempResults = Ds18b20.compensateTemperature(RAW_TEMPERATURE, TEMP_CALIBRATION);
        final float pressure = Ds18b20.compensatePressure(RAW_PRESSURE, tempResults[1],
                PRESSURE_CALIBRATION);
        Assert.assertEquals(EXPECTED_PRESSURE, pressure, EXPECTED_PRESSURE * TOLERANCE);
    }

    @Test
    public void testCompensateHumidity() {
        final float[] tempResults = Ds18b20.compensateTemperature(RAW_TEMPERATURE, TEMP_CALIBRATION);
        final float humidity = Ds18b20.compensateHumidity(RAW_HUMIDITY, tempResults[1],
                HUM_CALIBRATION);
        Assert.assertEquals(EXPECTED_HUMIDITY, humidity, EXPECTED_HUMIDITY * TOLERANCE);
    }

    @Test
    public void close() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.close();
        Mockito.verify(mI2c).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.close();
        bmx280.close(); // should not throw
        Mockito.verify(mI2c, times(1)).close();
    }


    @Test
    public void setMode_throwsIfClosed() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.close();
        mExpectedException.expect(IllegalStateException.class);
        bmx280.setMode(Ds18b20.MODE_NORMAL);
    }

    @Test
    public void setTmperatureOversampling_throwsIfClosed() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.close();
        mExpectedException.expect(IllegalStateException.class);
        bmx280.setTemperatureOversampling(Ds18b20.OVERSAMPLING_1X);
    }

    @Test
    public void setPressureOversampling_throwsIfClosed() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.close();
        mExpectedException.expect(IllegalStateException.class);
        bmx280.setPressureOversampling(Ds18b20.OVERSAMPLING_1X);
    }


    @Test
    public void setHumidityOversampling_throwsIfClosed() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.close();
        mExpectedException.expect(IllegalStateException.class);
        bmx280.setHumidityOversampling(Ds18b20.OVERSAMPLING_1X);
    }

    @Test
    public void readTemperature() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.setTemperatureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.readTemperature();
        Mockito.verify(mI2c).readRegBuffer(eq(0xFA), any(byte[].class), eq(3));
    }

    @Test
    public void readTemperature_throwsIfTemperatureOversamplingSkipped() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        // setTemperatureOversampling() not called
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("temperature oversampling is skipped");
        bmx280.readTemperature();
    }

    @Test
    public void readTemperature_throwsIfClosed() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.setTemperatureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("not open");
        bmx280.readTemperature();
    }

    @Test
    public void readPressure() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.setTemperatureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.setPressureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.readTemperatureAndPressure();
        Mockito.verify(mI2c).readRegBuffer(eq(0xFA), any(byte[].class), eq(3));
        Mockito.verify(mI2c).readRegBuffer(eq(0xF7), any(byte[].class), eq(3));
    }

    @Test
    public void readPressure_throwsIfTemperatureOversamplingSkipped()
            throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        // setTemperatureOversampling() not called
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("temperature oversampling is skipped");
        bmx280.readTemperatureAndPressure();
    }

    @Test
    public void readPressure_throwsIfPressureOversamplingSkipped()
            throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.setTemperatureOversampling(Ds18b20.OVERSAMPLING_1X);
        // setPressureOversampling() not called
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("pressure oversampling is skipped");
        bmx280.readTemperatureAndPressure();
    }

    @Test
    public void readPressure_throwsIfClosed() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.setTemperatureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.setPressureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("not open");
        bmx280.readTemperature();
    }

    @Test
    public void readTemperatureAndPressure() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.setTemperatureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.setPressureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.readTemperatureAndPressure();
        Mockito.verify(mI2c).readRegBuffer(eq(0xFA), any(byte[].class), eq(3));
        Mockito.verify(mI2c).readRegBuffer(eq(0xF7), any(byte[].class), eq(3));
    }

    @Test
    public void readTemperatureAndPressure_throwsIfTemperatureOversamplingSkipped()
            throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        // setTemperatureOversampling() not called
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("temperature oversampling is skipped");
        bmx280.readTemperatureAndPressure();
    }

    @Test
    public void readTemperatureAndPressure_throwsIfPressureOversamplingSkipped()
            throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.setTemperatureOversampling(Ds18b20.OVERSAMPLING_1X);
        // setPressureOversampling() not called
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("pressure oversampling is skipped");
        bmx280.readTemperatureAndPressure();
    }

    @Test
    public void readTemperatureAndPressure_throwsIfClosed() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.setTemperatureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.setPressureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("not open");
        bmx280.readTemperature();
    }

    @Test
    public void readHumidity() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.setHasHumiditySensor(true);
        bmx280.setTemperatureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.setPressureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.setHumidityOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.readHumidity();
        Mockito.verify(mI2c).readRegBuffer(eq(0xFA), any(byte[].class), eq(3));
        Mockito.verify(mI2c).readRegBuffer(eq(0xFD), any(byte[].class), eq(2));
    }

    @Test
    public void readHumidity_throwsIfHumidityOversamplingSkipped()
            throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.setHasHumiditySensor(true);
        bmx280.setTemperatureOversampling(Ds18b20.OVERSAMPLING_1X);
        // setHumidityOversampling() not called
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("Humidity oversampling is skipped");
        bmx280.readHumidity();
    }

    @Test
    public void readHumidity_throwsIfTemperatureOversamplingSkipped()
            throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.setHasHumiditySensor(true);
        bmx280.setHumidityOversampling(Ds18b20.OVERSAMPLING_1X);
        // setTemperatureOversampling() not called
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("temperature oversampling is skipped");
        bmx280.readHumidity();
    }

    @Test
    public void readHumidity_throwsIfClosed() throws IOException {
        Ds18b20 bmx280 = new Ds18b20(mI2c);
        bmx280.setHasHumiditySensor(true);
        bmx280.setTemperatureOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.setHumidityOversampling(Ds18b20.OVERSAMPLING_1X);
        bmx280.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("not open");
        bmx280.readHumidity();
    }

}
