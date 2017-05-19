package org.iatrix.widgets;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IFilter;
import org.iatrix.Iatrix;
import org.iatrix.util.Helpers;
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
	private static long start_ms = System.currentTimeMillis();
	private static int current_limit = 0;
	private static int maxShownConsultations = Iatrix.CFG_MAX_SHOWN_CONSULTATIONS_DEFAULT;
	static Logger log = LoggerFactory.getLogger(KonsLoader.class);
	static List<Konsultation> konsList = new ArrayList<>();
	static List<KonsListComposite.KonsData> backgroundKonsDataList = new ArrayList<>();
	
	public Konsultation actKons;
	
	public KonsLoader(){
		super("KonsLoader");
		log.debug("loaderJob KonsLoader created");
	}
	
	public void setKons(Konsultation newKons){
		if (newKons == null && actKons == null) {
			log.debug("Already null actKons");
			return;
		}
		actKons = newKons;
		if (newKons != null) {
			this.patient = newKons.getFall().getPatient();
			log.debug(String.format("setKons newKons %s %s all %s", newKons.getId(),
				this.patient.getPersonalia(), Helpers.getShowAllConsultations()));
		}
	}
	
	private void showDuration(String msg){
		long now = System.currentTimeMillis();
		long duration_in_sec = (now - start_ms) / 1000;
		log.debug(msg + " after " + duration_in_sec + "." + ((now - start_ms) % 1000) + " secs for "
			+ konsList.size() + " konsData of " + this.patient.getPersonalia());
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor){
		synchronized (konsList) {
			start_ms = System.currentTimeMillis();
			konsList = new ArrayList<>();
			backgroundKonsDataList = new ArrayList<>();
			maxShownConsultations = Iatrix.CFG_MAX_SHOWN_CONSULTATIONS_DEFAULT;
			if (CoreHub.globalCfg != null) {
				maxShownConsultations = CoreHub.globalCfg.get(Iatrix.CFG_MAX_SHOWN_CONSULTATIONS,
					Iatrix.CFG_MAX_SHOWN_CONSULTATIONS_DEFAULT);
			}
			current_limit = maxShownConsultations;
			log.debug(String.format("run: all %s max %d current %d %s", Helpers.getShowAllConsultations(),
				maxShownConsultations, current_limit, patient.getPersonalia()));
			
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
							if (globalFilter == null || globalFilter.select(k)) {
								konsList.add(k);
								monitor.done();
							}
						}
						showDuration("after query " + konsList.size() + " entries");
					}
				}
			}
			if (monitor == null) {
				return Status.CANCEL_STATUS;
			}
			
			monitor.worked(1);
			if (monitor.isCanceled()) {
				monitor.done();
				return Status.CANCEL_STATUS;
			}
			
			monitor.worked(1);
			showDuration("before monitor.done");
			monitor.done();
			showDuration("after monitor.done all " + Helpers.getShowAllConsultations());
			for (Konsultation k : konsList) {
				KonsListComposite.KonsData ks = new KonsListComposite.KonsData(k);
				backgroundKonsDataList.add(ks);
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
			}	
			showDuration("after filling background " + backgroundKonsDataList.size() + " entries cancelled " + monitor.isCanceled());
	
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			} else {
				return Status.OK_STATUS;
				
			}
		}
	}
	
	public List<KonsListComposite.KonsData> getMoreKonsultationen(){
		current_limit += maxShownConsultations;
		if (backgroundKonsDataList.size() >= current_limit)
		{
			log.debug("getMoreKonsultationen " + backgroundKonsDataList.size() + " >= " + current_limit);
			return backgroundKonsDataList.subList(0, current_limit);
		}
		log.debug("getMoreKonsultationen " + backgroundKonsDataList.size() + " < " + current_limit + " all "  + Helpers.getShowAllConsultations());
		return convertKonsList2KonsData();
	}
	public List<KonsListComposite.KonsData> getKonsultationen(){
		log.debug("getKonsultationen all "  + Helpers.getShowAllConsultations());
		return convertKonsList2KonsData();
	}
	private List<KonsListComposite.KonsData> convertKonsList2KonsData() {
		// convert Konsultation objects to KonsData objects
		long start = System.currentTimeMillis();
		int i = 0; // counter for maximally shown charges
		List<KonsListComposite.KonsData> konsDataList = new ArrayList<>();
		synchronized (konsList) {
			if (konsList != null) {
				for (Konsultation k : konsList) {
					KonsListComposite.KonsData ks = new KonsListComposite.KonsData(k);
					konsDataList.add(ks);
					i++;
					if (!Helpers.getShowAllConsultations() && i > current_limit) {
						break;
					}
				}	
			}
		}
		long now = System.currentTimeMillis();
		long duration_in_sec = (now - start) / 1000;
		if (konsList.size() > 0) {
			log.debug("convertKonsList2KonsData  toook " + duration_in_sec + "." + ((now - start_ms) % 1000) + " secs for "
				+ i + " curr " + current_limit + " all "  + Helpers.getShowAllConsultations() + 
				" konsData of " + this.patient.getPersonalia());
		}
		return konsDataList;
	}
}