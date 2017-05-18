/*******************************************************************************
 * Copyright (c) 2007-2013, D. Lutz and Elexis.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     D. Lutz - initial API and implementation
 *     Gerry Weirich - adapted for 2.1
 *
 * Sponsors:
 *     Dr. Peter Schönbucher, Luzern
 ******************************************************************************/
package org.iatrix.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.ViewPart;
import org.iatrix.Iatrix;
import org.iatrix.Messages;
import org.iatrix.widgets.KonsListDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.data.events.ElexisEvent;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.ui.actions.GlobalActions;
import ch.elexis.core.ui.actions.GlobalEventDispatcher;
import ch.elexis.core.ui.actions.IActivationListener;
import ch.elexis.core.ui.events.ElexisUiEventListenerImpl;
import ch.elexis.core.ui.util.ViewMenus;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;

/**
 * View for showing Konsultationen
 *
 * @author danlutz
 */
public class KonsListView extends ViewPart implements IActivationListener, ISaveablePart2 {
	public static final String ID = "org.iatrix.views.KonsListView"; //$NON-NLS-1$

	private static final String VIEW_CONTEXT_ID = "org.iatrix.view.konslist.context"; //$NON-NLS-1$
	private static Logger log = LoggerFactory.getLogger(KonsListView.class);

	KonsListDisplay konsListDisplay;

	private Action showMoreConsultationsAction;
	private Action showAllConsultationsAction;
	private Konsultation actKons = null;
	private ViewMenus menus;

	private void displaySelectedConsultation(Konsultation newKons) {
		actKons = newKons;
		log.debug("KonstListView " + (newKons == null ? "null" : newKons.getLabel() + " for " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		newKons.getFall().getPatient().getPersonalia()));
		showMoreConsultationsAction.setChecked(false);
		showAllConsultationsAction.setChecked(false);
		konsListDisplay.setKonsultation(actKons, showMoreConsultationsAction.isChecked(),
			showAllConsultationsAction.isChecked());
	}
	private final ElexisUiEventListenerImpl eeli_pat =
			new ElexisUiEventListenerImpl(Patient.class, ElexisEvent.EVENT_SELECTED |
				ElexisEvent.EVENT_RELOAD | ElexisEvent.EVENT_UPDATE) {

				@Override
				public void runInUi(ElexisEvent ev){
					Patient newPat = (Patient) ev.getObject();
					Konsultation newCons = null;
					if (newPat != null ) {
						newCons = newPat.getLetzteKons(false);
					log.debug("eeli_pat " + newPat.getPersonalia() + " newCons " //$NON-NLS-1$ //$NON-NLS-2$
						+ (newCons == null ? "null" : newCons.getId() + " " + newCons.getLabel())); //$NON-NLS-1$ //$NON-NLS-2$
					} else {
					log.debug("eeli_pat newCons is null"); //$NON-NLS-1$
					}
					displaySelectedConsultation(newCons);
				}
			};

	private final ElexisUiEventListenerImpl eeli_kons = new ElexisUiEventListenerImpl(Konsultation.class) {
		@Override
		public void runInUi(ElexisEvent ev){
			Konsultation newKons = (Konsultation) ev.getObject();
			if (ev.getType() == ElexisEvent.EVENT_SELECTED) {
					String konsInfo = newKons.getFall().getPatient().getPersonalia() + " " //$NON-NLS-1$
						+ newKons.getId() + " " + newKons.getLabel(); //$NON-NLS-1$
					log.debug("eeli_kons EVENT_SELECTED " + konsInfo); //$NON-NLS-1$
				showMoreConsultationsAction.setChecked(false);
				showAllConsultationsAction.setChecked(false);
				displaySelectedConsultation(newKons);
			}
		}
	};

	@Override
	public void createPartControl(Composite parent){
		parent.setLayout(new FillLayout());
		konsListDisplay = new KonsListDisplay(parent);

		makeActions();
		menus = new ViewMenus(getViewSite());
		menus.createMenu(showAllConsultationsAction, showMoreConsultationsAction);
		// menus.createToolbar(showAllConsultationsAction, showAllChargesAction);

		GlobalEventDispatcher.addActivationListener(this, this);
		activateContext();
	}

	/**
	 * Activate a context that this view uses. It will be tied to this view activation events and
	 * will be removed when the view is disposed. Copied from
	 * org.eclipse.ui.examples.contributions.InfoView.java
	 */
	private void activateContext(){
		IContextService contextService =
			(IContextService) getSite().getService(IContextService.class);
		contextService.activateContext(VIEW_CONTEXT_ID);
	}

	private void makeActions(){
		showMoreConsultationsAction =
			new Action(Messages.KonsListView_show_more_consultations, Action.AS_CHECK_BOX) {
			{
				setChecked(false);
					setToolTipText(Messages.KonsListView_show_more_consultations_tooltip);
			}

			@Override
			public void run(){
				boolean showAllCharges = this.isChecked();
				konsListDisplay.setKonsultation(actKons,
					showAllCharges, showAllConsultationsAction.isChecked());
			}
		};
		showMoreConsultationsAction.setActionDefinitionId(Iatrix.SHOW_MORE_CONSULTATIONS_COMMAND);
		GlobalActions.registerActionHandler(this, showMoreConsultationsAction);
		showAllConsultationsAction =
			new Action(Messages.KonsListView_show_all_consultations, Action.AS_CHECK_BOX) {
				{
					setChecked(false);
					setToolTipText(Messages.KonsListView_show_all_consultations_tooltip);
				}

				@Override
				public void run(){
					konsListDisplay.setKonsultation(actKons,
						showMoreConsultationsAction.isChecked(), showAllConsultationsAction.isChecked());
				}
			};
		showAllConsultationsAction.setActionDefinitionId(Iatrix.SHOW_ALL_CONSULTATIONS_COMMAND);
		GlobalActions.registerActionHandler(this, showAllConsultationsAction);
	}

	@Override
	public void setFocus(){
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose(){
		GlobalEventDispatcher.removeActivationListener(this, this);
		super.dispose();
	}

	@Override
	public void activation(boolean mode){
	// do nothing
	}

	@Override
	public void visible(boolean mode){
		if (mode == true) {
			ElexisEventDispatcher.getInstance().addListeners(eeli_pat, eeli_kons);
			Konsultation newKons = (Konsultation) ElexisEventDispatcher.getSelected(Konsultation.class);
			String msg = newKons == null ? "null" //$NON-NLS-1$
					: newKons.getId() + " " + newKons.getLabel() + " " //$NON-NLS-1$ //$NON-NLS-2$
						+ newKons.getFall().getPatient().getPersonalia();
			log.debug("visible true anlog eeli_kons" + msg); //$NON-NLS-1$
			showMoreConsultationsAction.setChecked(false);
			showAllConsultationsAction.setChecked(false);
			displaySelectedConsultation(newKons);
		} else {
			ElexisEventDispatcher.getInstance().removeListeners(eeli_pat, eeli_kons);
		}
	}

	/* ******
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2 Wir benötigen das
	 * Interface nur, um das Schliessen einer View zu verhindern, wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */
	@Override
	public int promptToSaveOnClose(){
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL
				: ISaveablePart2.NO;
	}

	@Override
	public void doSave(IProgressMonitor monitor){ /* leer */}

	@Override
	public void doSaveAs(){ /* leer */}

	@Override
	public boolean isDirty(){
		return true;
	}

	@Override
	public boolean isSaveAsAllowed(){
		return false;
	}

	@Override
	public boolean isSaveOnCloseNeeded(){
		return true;
	}
}
