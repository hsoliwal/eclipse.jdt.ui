/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import java.util.Arrays;
import java.util.Random;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 *
 * @since 3.2
 */
public class LineTrackerPerformanceTest extends TextPerformanceTestCase {

	protected static final String FAUST1;
	protected static final String FAUST500;

	static {
		FAUST1 = AbstractDocumentLineDifferTest.getFaust();

		FAUST500= FAUST1.substring(0, 500);
	}


	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(LineTrackerPerformanceTest.class));
	}

	public static Test setUpTest(Test test) {
		return new PerformanceTestSetup(test);
	}

	protected ILineTracker createLineTracker() {
		return new DefaultLineTracker();
	}

	private ILineTracker fLineTracker;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(20);
		setMeasuredRuns(20);

		fLineTracker= createLineTracker();
	}

	public void testSet() throws Exception {
		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureSet(meter);

		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureSet(meter);

		commitAllMeasurements();
		assertAllPerformance();

	}

	private void measureSet(PerformanceMeter meter) {
		meter.start();
		for (int accumulated= 0; accumulated < 100; accumulated++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			fLineTracker.set(FAUST1);
		}
		meter.stop();
	}

	public void testReplace() throws Exception {
		fLineTracker.set(FAUST1);

		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureReplace(meter);

		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureReplace(meter);

		commitAllMeasurements();
		assertAllPerformance();

	}

	private void measureReplace(PerformanceMeter meter) throws BadLocationException {
		meter.start();
		for (int times= 0; times < 15000; times++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			fLineTracker.replace(12, 500, FAUST500);
		meter.stop();
	}

	public void testRandomReplace() throws Exception {
		fLineTracker.set(FAUST1);

		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureRandomReplace(meter);

		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureRandomReplace(meter);

		commitAllMeasurements();
		assertAllPerformance();

	}

	private void measureRandomReplace(PerformanceMeter meter) throws BadLocationException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		Random rand= new Random(132498029834234L);
		int[] indices= new int[1000];
		for (int i= 0; i < indices.length; i++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			indices[i]= rand.nextInt(FAUST1.length() - 100);
		char[] replacement= new char[100];
		Arrays.fill(replacement, ' ');
		String replace= new String(replacement);

		meter.start();
		for (int times= 0; times < 300; times++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			for (int index : indices) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				fLineTracker.replace(index, 100, null);
				fLineTracker.replace(index, 0, replace);
			}
		}
		meter.stop();
	}

	public void testLineByOffset() throws Exception { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		fLineTracker.set(FAUST1);
		fLineTracker.replace(0, 0, ""); // trigger replacement with TreeLineTracker implementation

		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureLineByOffset(meter);

		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureLineByOffset(meter);

		commitAllMeasurements();
		assertAllPerformance();

	}

	private void measureLineByOffset(PerformanceMeter meter) throws BadLocationException {
		int chars= FAUST1.length();
		meter.start();
		for (int times= 0; times < 30; times++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			for (int offset= 0; offset <= chars; offset++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				fLineTracker.getLineNumberOfOffset(offset);
		meter.stop();
	}

	public void testLineByOffset2() throws Exception {
		fLineTracker.set(FAUST1);

		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureLineByOffset2(meter);

		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureLineByOffset2(meter);

		commitAllMeasurements();
		assertAllPerformance();

	}

	private void measureLineByOffset2(PerformanceMeter meter) throws BadLocationException {
		int chars= FAUST1.length();
		meter.start();
		for (int times= 0; times < 30; times++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			for (int offset= 0; offset <= chars; offset++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				fLineTracker.getLineNumberOfOffset(offset);
		meter.stop();
	}

	public void testLineByIndex() throws Exception { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		fLineTracker.set(FAUST1);
		fLineTracker.replace(0, 0, ""); // trigger replacement with TreeLineTracker implementation

		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureLineByIndex(meter);

		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureLineByIndex(meter);

		commitAllMeasurements();
		assertAllPerformance();

	}

	private void measureLineByIndex(PerformanceMeter meter) throws BadLocationException {
		int lines= fLineTracker.getNumberOfLines();
		meter.start();
		for (int times= 0; times < 1000; times++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			for (int line= 0; line < lines; line++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				fLineTracker.getLineOffset(line);
		meter.stop();
	}

	public void testLineByIndex2() throws Exception {
		fLineTracker.set(FAUST1);

		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureLineByIndex2(meter);

		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			measureLineByIndex2(meter);

		commitAllMeasurements();
		assertAllPerformance();

	}

	private void measureLineByIndex2(PerformanceMeter meter) throws BadLocationException {
		int lines= fLineTracker.getNumberOfLines();
		meter.start();
		for (int times= 0; times < 1000; times++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			for (int line= 0; line < lines; line++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				fLineTracker.getLineOffset(line);
		meter.stop();
	}
}
