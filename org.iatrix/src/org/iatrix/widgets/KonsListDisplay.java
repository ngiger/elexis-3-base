/*******************************************************************************
 * Copyright (c) 2007-2013, D. Lutz and Elexis.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    D. Lutz    - new version for Iatrix
 *    G. Weirich - adapted to API-Changes
 *
 * Sponsors:
 *     Dr. Peter Schönbucher, Luzern
 ******************************************************************************/
package org.iatrix.widgets;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.iatrix.widgets.KonsListComposite.KonsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.ui.util.SWTHelper;
import ch.elexis.data.Konsultation;

/**
 * Anzeige der vergangenen Konsultationen inkl. Verrechnung. Der Patient wird ueber die Methode
 * setPatient(Patient) festgelegt.
 *
 * @author Daniel Lutz
 *
 */
public class KonsListDisplay extends Composite implements IJobChangeListener, IJournalArea {

	private final FormToolkit toolkit;
	private final ScrolledForm form;
	private final Composite formBody;

	private final KonsListComposite konsListComposite;

	private final KonsLoader dataLoader;
	private static Logger log = LoggerFactory.getLogger(org.iatrix.widgets.KonsListDisplay.class);

	// if false, only show the charges of the latest 2 consultations
	private boolean showAllCharges = false;

	// if false, only show the latest MAX_SHOWN_CONSULTATIONS
	private boolean showAllConsultations = false;

	public KonsListDisplay(Composite parent){
		super(parent, SWT.BORDER);

		setLayout(new FillLayout());

		toolkit = new FormToolkit(getDisplay());
		form = toolkit.createScrolledForm(this);
		formBody = form.getBody();

		formBody.setLayout(new TableWrapLayout());

		konsListComposite = new KonsListComposite(formBody, toolkit);
		konsListComposite.setLayoutData(SWTHelper.getFillTableWrapData(1, true, 1, false));

		dataLoader = new KonsLoader();
		dataLoader.actKons = actKons;
		dataLoader.addJobChangeListener(this);
	}

	/**
	 * reload contents
	 * @param object
	 */
	private void reload(boolean showLoading, List<KonsData> konsultationen){
		if (actKons!= null && konsultationen != null) {
			konsListComposite.setKonsultationen(konsultationen, actKons);
		} else {
			if (konsultationen == null) {
				konsListComposite.setKonsultationen(null, null);
			} else if (showLoading ) {
				konsListComposite.setKonsultationen(null, null);
			}
		}
		refresh();
	}

	/*
	 * re-display data
	 */
	private void refresh(){
		// check for disposed widget to avoid error message at program exit
		if (form.isDisposed()) {
			dataLoader.cancel();
		} else {
			form.reflow(true);
		}
	}

	@Override
	public void visible(boolean mode){
		actKons = (Konsultation) ElexisEventDispatcher.getSelected(Konsultation.class);
		if (actKons == null) {
			konsListComposite.setKonsultationen(null, null);
		} else {
			final List<KonsData> copy = new ArrayList<>(dataLoader.getKonsultationen());
			log.debug("visible konsListDisplay " + mode + " " + (actKons != null ? actKons.getId() +
					" " + actKons.getFall().getPatient().getPersonalia() 
					: "null") +
				" with " + copy.size() + " dataLoader.getKonsultationen + konsListComposite.setKonsultationen");
			konsListComposite.setKonsultationen(copy, actKons);
			reload(false, copy);
			setKons(actKons, KonsActions.ACTIVATE_KONS);
		}
	}

	@Override
	public void activation(boolean mode){
	}

	static int savedKonsVersion = -1;
	static Konsultation actKons = null;

	private static boolean savedShowCharges = false;
	private static boolean savedshowConsultations = false;
	@Override
	public void setKons(Konsultation newKons, KonsActions op){
		if (newKons == null) {
			log.debug("setKons reload null");
			actKons = newKons;
			reload(false, null);
		} else {
			if (newKons != null && actKons != null && newKons.getId().equals(actKons.getId()) &&
					(savedShowCharges == showAllCharges) &&
					(savedshowConsultations == showAllConsultations)
					) {
				log.debug("setKons konsId matches skip reload");
			} else {
				String konsInfo = "actKons " + ( actKons != null ? actKons.getId() + actKons.getLabel() : "null") + 
						newKons.getId() + " " + newKons.getLabel() +
						" " + newKons.getFall().getPatient().getPersonalia();
				log.debug("setKons " +konsInfo);
				actKons = newKons;
				dataLoader.cancel();
				reload(true, null);
				dataLoader.setKons(newKons, showAllCharges, showAllConsultations);
				dataLoader.schedule();
			}
		}
		savedShowCharges = showAllCharges;
		savedshowConsultations = showAllConsultations;
		konsListComposite.refeshHyperLinks(actKons);
	}

	public void worked(int work){
		log.debug("loaderJob worked for " + dataLoader.getKonsultationen().size()  + " kons.");
	/* empty */}

	@Override
	public void aboutToRun(IJobChangeEvent event) {
		int nrKons = dataLoader.getKonsultationen().size() ;
		log.debug("loaderJob aboutToRun for " + nrKons + " kons.");
	/* empty */}

	@Override
	public void awake(IJobChangeEvent event) {
		int nrKons = dataLoader.getKonsultationen().size() ;
		log.debug("loaderJob awake for " + nrKons + " kons.");
	/* empty */}

	@Override
	public void done(IJobChangeEvent event) {
		final List<KonsData> copy = new ArrayList<>(dataLoader.getKonsultationen());
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				int nrKonst = copy.size();
				System.out.println("loaderJob got done for " + nrKonst  + " kons.");
				reload(false, copy);
			}
		});
	}

	@Override
	public void running(IJobChangeEvent event) {
	/* empty */}

	@Override
	public void scheduled(IJobChangeEvent event) {
	/* empty */}

	@Override
	public void sleeping(IJobChangeEvent event) {
	/* empty */}

	public void setKonsultation(Konsultation newKons, boolean showCharges, boolean showConsultations){
		showAllCharges = showCharges;
		showAllConsultations = showConsultations;
		setKons(newKons, KonsActions.ACTIVATE_KONS);
	}
}
