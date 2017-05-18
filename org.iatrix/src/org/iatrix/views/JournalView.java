/*******************************************************************************
 * Copyright (c) 2007-2015, D. Lutz and Elexis.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     D. Lutz - initial API and implementation
 *     Gerry Weirich - adapted for 2.1
 *     Niklaus Giger - small improvements, split into 20 classes
 *
 * Sponsors:
 *     Dr. Peter Schönbucher, Luzern
 ******************************************************************************/
package org.iatrix.views;

import static ch.elexis.core.data.events.ElexisEvent.EVENT_DESELECTED;
import static ch.elexis.core.data.events.ElexisEvent.EVENT_RELOAD;
import static ch.elexis.core.data.events.ElexisEvent.EVENT_SELECTED;
import static ch.elexis.core.data.events.ElexisEvent.EVENT_UPDATE;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.iatrix.Iatrix;
import org.iatrix.Messages;
import org.iatrix.data.KonsTextLock;
import org.iatrix.util.Constants;
import org.iatrix.util.Heartbeat;
import org.iatrix.util.Helpers;
import org.iatrix.widgets.IJournalArea;
import org.iatrix.widgets.IJournalArea.KonsActions;
import org.iatrix.widgets.JournalHeader;
import org.iatrix.widgets.KonsDiagnosen;
import org.iatrix.widgets.KonsHeader;
import org.iatrix.widgets.KonsListDisplay;
import org.iatrix.widgets.KonsProblems;
import org.iatrix.widgets.KonsText;
import org.iatrix.widgets.KonsVerrechnung;
import org.iatrix.widgets.ProblemArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.events.ElexisEvent;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.ui.UiDesk;
import ch.elexis.core.ui.actions.GlobalActions;
import ch.elexis.core.ui.actions.GlobalEventDispatcher;
import ch.elexis.core.ui.actions.IActivationListener;
import ch.elexis.core.ui.events.ElexisUiEventListenerImpl;
import ch.elexis.core.ui.icons.Images;
import ch.elexis.core.ui.util.SWTHelper;
import ch.elexis.core.ui.util.ViewMenus;
import ch.elexis.data.Anwender;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.extdoc.util.Email;
import ch.elexis.icpc.Episode;
import ch.rgw.tools.TimeTool;
import de.kupzog.ktable.KTable;

/**
 * KG-Ansicht nach Iatrix-Vorstellungen
 *
 * Oben wird die Problemliste dargestellt, unten die aktuelle Konsultation und die bisherigen
 * Konsultationen. Hinweis: Es wird sichergestellt, dass die Problemliste und die Konsultation(en)
 * zum gleichen Patienten gehoeren.
 *
 * @author Daniel Lutz <danlutz@watz.ch> Original implementation (Up to Elexis 2.0)
 * 		   Niklaus Giger <niklaus.giger@member.fsf.org> Reworked for Elexis 3.x
 */

public class JournalView extends ViewPart implements IActivationListener, ISaveablePart2 {

	/**
	 *  ID of the Journal View
	 */
	public static final String ID = Constants.ID;

	private static Logger log = LoggerFactory.getLogger(JournalView.class);
	private static Patient actPatient = null;
	private static Konsultation actKons = null;
	private static boolean removedStaleKonsLocks = false;

	private FormToolkit tk;
	private Form form;

	// container for hKonsultationDatum, hlMandant, cbFall

	// Parts (from top to bottom that make up our display
	private JournalHeader formHeader = null; // Patient name, sex, birthday, remarks, sticker, account, balance, account overview
	private KTable problemsKTable = null; // On top
	private ProblemArea problemsArea = null; // KTable with Date, nr, diagnosis, therapy, code, activ/inactiv
	private KonsProblems konsProblems = null; // left: List of Checkbox of all problems for this consultation
	private KonsText konsTextComposite; // Konsultationtext (with lock over all stations), revision info
	private KonsVerrechnung konsVerrechnung = null; // right: Items to be billed for select consultation
	private KonsDiagnosen konsDiagnosen = null; // diagnosis line
	private KonsListDisplay konsListDisplay; // bottom list of all consultations date, decreasing with date, mandant, case, text, billed items

	private ViewMenus menus;

	/* Actions */
	private IAction exportToClipboardAction;
	private IAction sendEmailAction;
	private IAction addKonsultationAction;
	private Action showMoreConsultations;
	private Action showAllConsultationsAction;

	private static List<IJournalArea> allAreas;

	private KonsHeader konsHeader;

	private Heartbeat heartbeat;

	@Override
	public void createPartControl(Composite parent){
		parent.setLayout(new FillLayout());
		heartbeat = Heartbeat.getInstance();
		tk = UiDesk.getToolkit();
		form = tk.createForm(parent);
		Composite formBody = form.getBody();

		formBody.setLayout(new GridLayout(1, true));
		formHeader = new JournalHeader(formBody);

		SashForm mainSash = new SashForm(form.getBody(), SWT.VERTICAL);
		mainSash.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));

		Composite topArea = tk.createComposite(mainSash, SWT.NONE);
		topArea.setLayout(new FillLayout(SWT.VERTICAL));
		topArea.setBackground(topArea.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		problemsArea = new ProblemArea(topArea, JournalView.this.getPartName(), getViewSite());
		problemsKTable = problemsArea.getProblemKTable();
		Composite middleArea = tk.createComposite(mainSash, SWT.NONE);
		middleArea.setLayout(new FillLayout());
		Composite konsultationComposite = tk.createComposite(middleArea);
		konsultationComposite.setLayout(new GridLayout(1, true));

		konsHeader = new KonsHeader(konsultationComposite);

		SashForm konsultationSash = new SashForm(konsultationComposite, SWT.HORIZONTAL);
		konsultationSash.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));

		Composite assignmentComposite = tk.createComposite(konsultationSash);
		assignmentComposite.setLayout(new GridLayout(1, true));
		konsProblems = new KonsProblems(assignmentComposite); // on the left side
		Composite konsultationTextComposite = tk.createComposite(konsultationSash);
		konsultationTextComposite.setLayout(new GridLayout(1, true));
		konsTextComposite = new KonsText(konsultationTextComposite);
		konsDiagnosen = new KonsDiagnosen(konsultationComposite);
		Composite verrechnungComposite = tk.createComposite(konsultationSash);
		konsVerrechnung = new KonsVerrechnung(verrechnungComposite, form,
			JournalView.this.getPartName(), assignmentComposite);
		if (konsultationSash.getChildren().length == 3) {
			konsultationSash.setWeights(new int[] {
				15, 65, 20
			});
		} else {
			// log.debug("konsSash should have 3, but has " + konsultationSash.getChildren().length + " children");
		}
		Composite bottomArea = tk.createComposite(mainSash, SWT.NONE);
		bottomArea.setLayout(new FillLayout());
		bottomArea.setBackground(bottomArea.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		konsListDisplay = new KonsListDisplay(bottomArea);

		mainSash.setWeights(new int[] {
			20, 40, 30
		});
		allAreas = new ArrayList<>();
		allAreas.add(konsTextComposite); // Let the konsText be available for input as soon as possible
		allAreas.add(formHeader);
		allAreas.add(problemsArea);
		allAreas.add(konsHeader);
		allAreas.add(konsProblems);
		allAreas.add(konsDiagnosen);
		allAreas.add(konsVerrechnung);
		allAreas.add(konsListDisplay);
		makeActions();
		menus = new ViewMenus(getViewSite());
		if (CoreHub.acl.request(AccessControlDefaults.AC_PURGE)) {
			menus.createMenu(addKonsultationAction, GlobalActions.redateAction, problemsArea.addProblemAction,
				GlobalActions.delKonsAction, problemsArea.delProblemAction, exportToClipboardAction, sendEmailAction,
				konsTextComposite.getVersionForwardAction(), konsTextComposite.getVersionBackAction(),
				konsTextComposite.getChooseVersionAction(), konsTextComposite.getPurgeAction(),
				konsTextComposite.getSaveAction(), showAllConsultationsAction, showMoreConsultations,
				problemsArea.addFixmedikationAction);
		} else {
			menus.createMenu(addKonsultationAction, GlobalActions.redateAction, problemsArea.addProblemAction,
				GlobalActions.delKonsAction, problemsArea.delProblemAction, exportToClipboardAction, sendEmailAction,
				konsTextComposite.getVersionForwardAction(), konsTextComposite.getVersionBackAction(),
				konsTextComposite.getChooseVersionAction(), konsTextComposite.getSaveAction(),
				showAllConsultationsAction, showMoreConsultations, problemsArea.addFixmedikationAction);
		}

		menus.createToolbar(sendEmailAction, exportToClipboardAction, addKonsultationAction,
			problemsArea.getAddProblemAction(), konsTextComposite.getSaveAction());
		menus.createViewerContextMenu(konsProblems.getProblemAssignmentViewer(), konsProblems.unassignProblemAction);
		menus.createViewerContextMenu(konsVerrechnung.getVerrechnungViewer(),
			konsVerrechnung.changeVerrechnetPreisAction, konsVerrechnung.changeVerrechnetZahlAction,
			konsVerrechnung.delVerrechnetAction);

		GlobalEventDispatcher.addActivationListener(this, this);
		activateContext();
	}

	/**
	 * Save actual Kons
	 */
	public static void saveActKonst(){
		if (actKons == null) {
			return;
		}
		logEvent(actKons, "saveActKonst"); //$NON-NLS-1$
		for (int i = 0; i < allAreas.size(); i++) {
			IJournalArea a = allAreas.get(i);
			if (a != null) {
				a.setKons(actKons, KonsActions.SAVE_KONS);
			}
		}
	}

	/**
	 * First ste the global variable actKons
	 * Then updates all dependent widgets, like header, konsText konsList
	 * @param newKons
	 * @param op
	 */
	public static void updateAllKonsAreas(Konsultation newKons, IJournalArea.KonsActions op){
		/*
		 * Not yet sure whether comparing only the id or the whole cons is better
		 */
		actKons = newKons;
		if (newKons == null) {
			return;
		}
		// It is a bad idea to skip updating the kons, when the Id matches
		// Some changes, e.g. when date of actual kons are possible even when the compare matches.
		// Therefore we return only when we have nothing to update savedKonst == newKons?" + newId + " konsId match? " + savedKonsId.equals(newId));
		logEvent(newKons, "updateAllKonsAreas: newKons"); //$NON-NLS-1$
		for (int i = 0; i < allAreas.size(); i++) {
			IJournalArea a = allAreas.get(i);
			if (a != null) {
				a.setKons(newKons, op);
			}
		}
	}

	private void activateAllKonsAreas(boolean mode){
		logEvent(null, "activateAllKonsAreas: " + mode); //$NON-NLS-1$
		for (int i = 0; i < allAreas.size(); i++) {
			IJournalArea a = allAreas.get(i);
			if (a != null) {
				a.activation(mode);
			}
		}
	}

	private void visibleAllKonsAreas(boolean mode){
		logEvent(null, "visibleAllKonsAreas: " + mode); //$NON-NLS-1$
		for (int i = 0; i < allAreas.size(); i++) {
			IJournalArea a = allAreas.get(i);
			if (a != null) {
				a.visible(mode);
			}
		}
	}

	private final ElexisUiEventListenerImpl eeli_problem =
		new ElexisUiEventListenerImpl(Episode.class, EVENT_UPDATE | EVENT_DESELECTED) {

			@Override
			public void runInUi(ElexisEvent ev){
				switch (ev.getType()) {
				case EVENT_UPDATE:
					// problem change may affect current problems list and consultation
					// TODO check if problem is part of current consultation
					// work-around: just update the current patient and consultation
					logEvent(null, "eeli_problem EVENT_UPDATE"); //$NON-NLS-1$
					problemsArea.reloadAndRefresh();
					break;
				case EVENT_DESELECTED:
					logEvent(null, "eeli_problem EVENT_DESELECTED"); //$NON-NLS-1$
					problemsKTable.clearSelection();
					break;
				}

			}
		};

	private final ElexisUiEventListenerImpl eeli_kons =
			new ElexisUiEventListenerImpl(Konsultation.class,
				EVENT_SELECTED | EVENT_UPDATE | EVENT_RELOAD) {

		@Override
		public void runInUi(ElexisEvent ev){
			Konsultation newKons = (Konsultation) ev.getObject();
				String msg = "unknown"; //$NON-NLS-1$
			switch (ev.getType()) {
			case EVENT_SELECTED:
					msg = "EVENT_SELECTED"; //$NON-NLS-1$
				break;
			case EVENT_UPDATE:
					msg = "EVENT_UPDATE"; //$NON-NLS-1$
				break;
			case EVENT_RELOAD:
					msg = "EVENT_RELOAD"; //$NON-NLS-1$
				break;
			}
			if (!removedStaleKonsLocks) {
				removedStaleKonsLocks = true;
				KonsTextLock.deleteObsoleteLocks(newKons);
			}
			// when we get an update or select event the parameter is always not null
			if ((actKons == null) || !Helpers.haveSameContent(newKons, actKons)) {
					logEvent(newKons, "eeli_kons " + msg + " SAVE_KONS"); //$NON-NLS-1$ //$NON-NLS-2$
				// updateAllKonsAreas(actKons, KonsActions.SAVE_KONS);
				Patient newPatient = newKons.getFall().getPatient();
				if (newPatient != actPatient) {
						displaySelectedPatient(newPatient, "eeli_kons newPatient"); //$NON-NLS-1$
				}
					logEvent(newKons, "eeli_kons " + msg + " ACTIVATE_KONS"); //$NON-NLS-1$ //$NON-NLS-2$
				updateAllKonsAreas(newKons, KonsActions.ACTIVATE_KONS);
			} else {
				// Or we would simply forget to update it after
				// add items via a konsText makro
				konsVerrechnung.setKons(newKons, KonsActions.ACTIVATE_KONS);
			}
			actKons = newKons;
		}

	};

	/**
	 * Helper to update every thing whether we got notified by opening the view
	 * or the selected patient changed
	 *
	 * @param selectedPatient patient to be displayed
	 * @param why 		Where do we come from (Only used for the logging)
	 */
	private void displaySelectedPatient(Patient selectedPatient, String why){
		if (selectedPatient == null) {
			logEvent(null, why + " displaySelectedPatient " + "no patient"); //$NON-NLS-1$ //$NON-NLS-2$
			actPatient = null;
			updateAllKonsAreas(null, KonsActions.ACTIVATE_KONS);
			return;

		} else {
			logEvent(null, why + " displaySelectedPatient " + selectedPatient.getId() //$NON-NLS-1$
				+ selectedPatient.getPersonalia());
		}

		showMoreConsultations.setChecked(false);
		showAllConsultationsAction.setChecked(false);

		// Find the most recent open konsultation for the given fall
		// If nothing found or not of today, create a new konsultation
		Konsultation konsultation = null;
		konsultation = selectedPatient.getLetzteKons(false);
		if (konsultation == null) {
			Fall[] faelle = selectedPatient.getFaelle();
			if (faelle.length == 0) {
				konsultation = selectedPatient.createFallUndKons();
			} else {
				for (Fall fall : faelle) {
					if (fall.isOpen()) {
						konsultation = fall.getLetzteBehandlung();
						if (konsultation == null) {
							konsultation = fall.neueKonsultation();
						} else {
							TimeTool konsDate = new TimeTool(konsultation.getDatum());
							if (!konsDate.isSameDay(new TimeTool())) {
								konsultation = konsultation.getFall().neueKonsultation();
							}
						}
						log.debug("displaySelectedPatient neue Kons fall.isOpen " //$NON-NLS-1$
							+ konsultation.getId() + " " + konsultation.getLabel()); //$NON-NLS-1$
						break;
					}
				}
				if (konsultation == null) {
					konsultation = selectedPatient.createFallUndKons();
					log.debug("displaySelectedPatient neue Kons createFallUndKons " //$NON-NLS-1$
						+ konsultation.getId() + " " + konsultation.getLabel()); //$NON-NLS-1$
				}
			}
		}
		TimeTool konsDate = new TimeTool(konsultation.getDatum());
		if (!konsDate.isSameDay(new TimeTool())) {
			konsultation = konsultation.getFall().neueKonsultation();
		}
		// actKons = konsultation;
		// We do not call updateAllKonsAreas(actKons, KonsActions.ACTIVATE_KONS);
		// as this would overwrite the konstext when we change the patient and continue typing
		// See Ticket #5696
	}

	private final ElexisUiEventListenerImpl eeli_pat =
		// Soll hier auch noch auf RELOAD und UPDATE reagiert werden
		new ElexisUiEventListenerImpl(Patient.class, ElexisEvent.EVENT_SELECTED | ElexisEvent.EVENT_RELOAD | ElexisEvent.EVENT_UPDATE) {

			@Override
			public void runInUi(ElexisEvent ev){
				displaySelectedPatient((Patient) ev.getObject(), "eeli_pat " + ev.getType()); //$NON-NLS-1$
				// setPatient((Patient) ev.getObject());
			}
		};

	private final ElexisUiEventListenerImpl eeli_user =
		new ElexisUiEventListenerImpl(Anwender.class, ElexisEvent.EVENT_USER_CHANGED) {
			@Override
			public void runInUi(ElexisEvent ev){
				logEvent(null, "runInUi eeli_user adaptMenus"); //$NON-NLS-1$
				adaptMenus();
			}
		};

	/**
	 * Activate a context that this view uses. It will be tied to this view activation events and
	 * will be removed when the view is disposed. Copied from
	 * org.eclipse.ui.examples.contributions.InfoView.java
	 */
	private void activateContext(){
		IContextService contextService =
				(IContextService) getSite().getService(IContextService.class);
		contextService.activateContext(Constants.VIEW_CONTEXT_ID);
	}

	@Override
	public void dispose(){
		GlobalEventDispatcher.removeActivationListener(this, this);
		ElexisEventDispatcher.getInstance().removeListeners(eeli_kons, eeli_problem,
			eeli_pat, eeli_user);
		super.dispose();
	}

	@Override
	public void setFocus(){}

	/**
	 * Adapt the menus (create/delete kons) according to the ACL settings
	 */
	public void adaptMenus(){
		konsVerrechnung.getVerrechnungViewer().getTable().getMenu()
			.setEnabled(CoreHub.acl.request(AccessControlDefaults.LSTG_VERRECHNEN));

		// TODO this belongs to GlobalActions itself (action creator)
		GlobalActions.delKonsAction
			.setEnabled(CoreHub.acl.request(AccessControlDefaults.KONS_DELETE));
		GlobalActions.neueKonsAction
			.setEnabled(CoreHub.acl.request(AccessControlDefaults.KONS_CREATE));
	}

	private void makeActions(){
		// Konsultation

		// Replacement for GlobalActions.neueKonsAction (other image)
		addKonsultationAction = new Action(GlobalActions.neueKonsAction.getText()) {
			{
				setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin("org.iatrix", //$NON-NLS-1$
					"icons/new_konsultation.ico")); //$NON-NLS-1$
				setToolTipText(GlobalActions.neueKonsAction.getToolTipText());
			}

			@Override
			public void run(){
				GlobalActions.neueKonsAction.run();
			}
		};
		addKonsultationAction.setActionDefinitionId(Constants.NEWCONS_COMMAND);
		GlobalActions.registerActionHandler(this, addKonsultationAction);

		// Probleme
		if (problemsArea != null) {
			GlobalActions.registerActionHandler(this, problemsArea.addProblemAction);
			problemsArea.addProblemAction.setActionDefinitionId(Constants.NEWPROBLEM_COMMAND);
		}

		exportToClipboardAction = new Action(Messages.JournalView_export_to_clipboard) {
			{
				setImageDescriptor(Images.IMG_EXPORT.getImageDescriptor());
				setToolTipText(Messages.JournalView_export_extract_to_clipboard);
			}

			@Override
			public void run(){
				Helpers.exportToClipboard(actPatient, null); // TODO: selected problem
			}
		};
		exportToClipboardAction.setActionDefinitionId(Constants.EXPORT_CLIPBOARD_COMMAND);
		GlobalActions.registerActionHandler(this, exportToClipboardAction);

		sendEmailAction = new Action(Messages.JournalView_send_email) {
			{
				setImageDescriptor(Images.IMG_MAIL.getImageDescriptor());
				setToolTipText(Messages.JournalView_open_mail_program_with_drugs);
			}

			@Override
			public void run(){
				Email.openMailApplication("", // No default to address //$NON-NLS-1$
					null, Helpers.exportToClipboard(actPatient, null), // TODO: selected problem
					null);

			}
		};
		sendEmailAction.setActionDefinitionId(Constants.EXPORT_SEND_EMAIL_COMMAND);
		GlobalActions.registerActionHandler(this, sendEmailAction);

		// history display
		showMoreConsultations = new Action(Messages.JournalView_show_more_consultations, Action.AS_CHECK_BOX) {
			{
				setChecked(false);
				setToolTipText(
					Messages.JournalView_show_more_consultations_tooltip);
			}

			@Override
			public void run(){
				konsListDisplay.setKonsultation(actKons, showMoreConsultations.isChecked(),
					showAllConsultationsAction.isChecked());
			}
		};
		showMoreConsultations.setActionDefinitionId(Iatrix.SHOW_MORE_CONSULTATIONS_COMMAND);
		GlobalActions.registerActionHandler(this, showMoreConsultations);

		showAllConsultationsAction = new Action(Messages.JournalView_show_all_consultations,
			Action.AS_CHECK_BOX) {
			{
				setChecked(false);
				setToolTipText(Messages.JournalView_show_all_consultations_tooltip);
			}

			@Override
			public void run(){
				konsListDisplay.setKonsultation(actKons, showMoreConsultations.isChecked(),
					showAllConsultationsAction.isChecked());
			}
		};
		showAllConsultationsAction.setActionDefinitionId(Iatrix.SHOW_ALL_CONSULTATIONS_COMMAND);
		GlobalActions.registerActionHandler(this, showAllConsultationsAction);
	}

	@Override
	public void activation(boolean mode){
		Konsultation selected_kons = (Konsultation) ElexisEventDispatcher.getSelected(Konsultation.class);
		if (selected_kons != null && actKons != null && !selected_kons.getId().equals(actKons.getId())) {
			// this should never happen
			logEvent(null, "activation " + mode + " sel: " + selected_kons.getLabel() + " act: " //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				+ actKons.getId());
			return;
		}
		if (mode == false) {
			// text is neither dirty nor changed.
			// If it es empty and nothing has been billed, we just delete this kons.
			if (actKons == null) {
				return;
			}
			boolean noLeistungen = actKons.getLeistungen() == null || actKons.getLeistungen().isEmpty();
			log.debug("Delete the kons? " + konsTextComposite.getPlainText().length() //$NON-NLS-1$
				+ " noLeistungen " + noLeistungen); //$NON-NLS-1$
			if (konsTextComposite.getPlainText().length() == 0 && (noLeistungen)) {
				Fall f = actKons.getFall();
				Konsultation[] ret = f.getBehandlungen(false);
				actKons.delete(true);
				if (ret.length == 1) {
                   /* Trying to remove the associated case got me into problems.
                    * Peter Schoenbucher argued on September, 2, 2015, that we should never
                    * delete a case, because the case holds the information which Krankenkasse is
                    * attached to this client. Therefore often the assistant opens a case before
                    * the consultation starts
                   */
				}
			}
		}
	}

	@Override
	public void visible(boolean mode){
		if (mode == true) {
			ElexisEventDispatcher.getInstance().addListeners(eeli_kons, eeli_problem, eeli_pat,
				eeli_user);
			Konsultation newKons = (Konsultation) ElexisEventDispatcher.getSelected(Konsultation.class);
			if (newKons != null) {
				String msg = newKons.getId() + " " + newKons.getLabel() + " " //$NON-NLS-1$//$NON-NLS-2$
					+ newKons.getFall().getPatient().getPersonalia();
				logEvent(newKons, "visible true " + msg); //$NON-NLS-1$
				updateAllKonsAreas(newKons, KonsActions.ACTIVATE_KONS);

			} else
			{
				logEvent(newKons, "visible true newKons is null"); //$NON-NLS-1$
				displaySelectedPatient(ElexisEventDispatcher.getSelectedPatient(), "view visible"); //$NON-NLS-1$
			}
			visibleAllKonsAreas(mode);
			heartbeat.enableListener(true);
		} else {
			heartbeat.enableListener(false);
			ElexisEventDispatcher.getInstance().removeListeners(eeli_kons, eeli_problem,
				eeli_pat, eeli_user);
		}
	};

	private static void logEvent(Konsultation kons, String msg){
		StringBuilder sb = new StringBuilder(msg);
		if (kons != null) {
			Fall f = kons.getFall();
			if (f != null) {
				Patient pat = f.getPatient();
				sb.append(" kons: " + kons.getId()); //$NON-NLS-1$
				sb.append(" vom " + kons.getDatum()); //$NON-NLS-1$
				sb.append(" " + pat.getId() + ": " + pat.getPersonalia()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		log.debug(sb.toString());
	}

	/***********************************************************************************************
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
