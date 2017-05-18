package org.iatrix.widgets;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IFilter;
import org.iatrix.Iatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.ui.actions.ObjectFilterRegistry;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.data.Query;

final class KonsLoader extends Job {

	String name;
	Patient patient = null;
	private boolean showAllCharges = true;
	private boolean showAllConsultations = true;
	static Logger log = LoggerFactory.getLogger(KonsLoader.class);
	List<KonsListComposite.KonsData> konsDataList = new ArrayList<>();

	public Konsultation actKons;

	public KonsLoader(){
		super("KonsLoader");
		log.debug("loaderJob KonsLoader created");
	}

	public void setKons(Konsultation newKons, boolean showAllCharges,
		boolean showAllConsultations){
		if (newKons == null && actKons == null) {
			log.debug("Already null actKons");
			return;
		} 
		actKons = newKons;
		log.debug(String.format("actKons %s newKons %s", actKons.getId(), newKons.getId()));
		if (newKons != null) {
			this.patient = newKons.getFall().getPatient();
			log.debug(String.format("Switch newKons %s %s", newKons.getId(), this.patient.getPersonalia()));
		}
		this.showAllCharges = showAllCharges;
		this.showAllConsultations = showAllConsultations;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		synchronized (konsDataList) {
			int maxShownConsultations = Iatrix.CFG_MAX_SHOWN_CONSULTATIONS_DEFAULT;
			int maxShownCharges = Iatrix.CFG_MAX_SHOWN_CHARGES_DEFAULT;
			if (CoreHub.globalCfg != null) {
				maxShownCharges = CoreHub.globalCfg.get(Iatrix.CFG_MAX_SHOWN_CHARGES,
					Iatrix.CFG_MAX_SHOWN_CHARGES_DEFAULT);
				maxShownCharges = CoreHub.globalCfg.get(Iatrix.CFG_MAX_SHOWN_CHARGES,
					Iatrix.CFG_MAX_SHOWN_CHARGES_DEFAULT);
			}
			log.debug(String.format("Start run: showAllConsultations %s %s maxShownCharges %d maxShownConsultations %d",
				showAllConsultations, showAllCharges, maxShownCharges, 	maxShownConsultations));

			log.debug("loaderJob started patient " + (patient == null ? "null" : patient.getPersonalia()) + 
				" actKons " +	actKons == null ? "null" : actKons.getLabel());
			konsDataList.clear();

			List<Konsultation> konsList = new ArrayList<>();

			if (patient != null) {
				Fall[] faelle = patient.getFaelle();

				if (faelle.length > 0) {
					IFilter globalFilter =
						ObjectFilterRegistry.getInstance().getFilterFor(Konsultation.class);

					Query<Konsultation> query = new Query<>(Konsultation.class);
					query.startGroup();
					for (Fall fall : faelle) {
						query.add("FallID", "=", fall.getId());
						query.or();
					}
					query.endGroup();
					query.orderBy(true, "Datum");
					List<Konsultation> kons = query.execute();
					if (monitor.isCanceled()) {
						monitor.done();
						return Status.CANCEL_STATUS;
					}

					if (kons != null) {
						for (Konsultation k : kons) {
							if ( globalFilter == null || globalFilter.select(k)) {
								konsList.add(k);
								monitor.done();
							}
						}
					}
				}
			}
			log.debug(String.format("after Reading konsList.size() %d", konsList.size()));
			if (monitor == null) {
				return Status.CANCEL_STATUS;
			}

			monitor.worked(1);
			if (!showAllConsultations && konsList.size() > maxShownConsultations) {
				// don't load all entries
				List<Konsultation> newList = new ArrayList<>();
				for (int i = 0; i < maxShownConsultations; i++) {
					newList.add(konsList.get(i));
				}
				konsList = newList;
			}
			log.debug(String.format("after checking konsList.size() %d", konsList.size()));

			if (monitor.isCanceled()) {
				monitor.done();
				return Status.CANCEL_STATUS;
			}

			// convert Konsultation objects to KonsData objects
			int i = 0; // counter for maximally shown charges
			for (Konsultation k : konsList) {
				KonsListComposite.KonsData ks =
					new KonsListComposite.KonsData(k, showAllCharges || i < maxShownCharges);
				konsDataList.add(ks);
				i++;
				if (!showAllCharges && i > maxShownCharges) { 
					break;
					}
			}

			log.debug(String.format("Done: showAllConsultations %s Charges %s maxShownConsultations konsList.size() %d max %d %d",
				showAllConsultations, showAllCharges, konsList.size(),	maxShownCharges, maxShownConsultations));

			monitor.worked(1);
			monitor.done();

			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			} else {
				return Status.OK_STATUS;
			}
		}
	}

	public List<KonsListComposite.KonsData> getKonsultationen(){
		return konsDataList;
	}
}