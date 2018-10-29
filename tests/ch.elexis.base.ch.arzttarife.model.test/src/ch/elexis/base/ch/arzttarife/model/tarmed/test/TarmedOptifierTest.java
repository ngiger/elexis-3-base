package ch.elexis.base.ch.arzttarife.model.tarmed.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import ch.elexis.base.ch.arzttarife.model.test.AllTestsSuite;
import ch.elexis.base.ch.arzttarife.tarmed.importer.TarmedReferenceDataImporter;
import ch.elexis.base.ch.arzttarife.tarmed.model.TarmedConstants;
import ch.elexis.base.ch.arzttarife.tarmed.model.TarmedExtension;
import ch.elexis.base.ch.arzttarife.tarmed.model.TarmedLeistung;
import ch.elexis.base.ch.arzttarife.tarmed.model.TarmedOptifier;
import ch.elexis.base.ch.ticode.TessinerCode;
import ch.elexis.core.model.IBilled;
import ch.elexis.core.model.ICoverage;
import ch.elexis.core.model.IEncounter;
import ch.elexis.core.model.IMandator;
import ch.elexis.core.model.IPatient;
import ch.elexis.core.model.IPerson;
import ch.elexis.core.model.builder.IContactBuilder;
import ch.elexis.core.model.builder.ICoverageBuilder;
import ch.elexis.core.model.builder.IEncounterBuilder;
import ch.elexis.core.services.IConfigService;
import ch.elexis.core.services.IModelService;
import ch.elexis.core.types.Gender;
import ch.elexis.core.utils.OsgiServiceUtil;
import ch.rgw.tools.Result;
import ch.rgw.tools.TimeTool;

public class TarmedOptifierTest {
	private static IModelService coreModelService;
	private static IMandator mandator;
	private static TarmedOptifier optifier;
	
	private static IPatient patGrissemann, patStermann, patOneYear, patBelow75;
	private static IEncounter konsGriss, konsSter, konsOneYear, konsBelow75;
	private static TarmedLeistung tlBaseFirst5Min, tlBaseXRay, tlBaseRadiologyHospital,
			tlUltrasound, tlAgeTo1Month, tlAgeTo7Years, tlAgeFrom7Years, tlGroupLimit1,
			tlGroupLimit2, tlAlZero;
	private static IEncounter konsPeriodStart, konsPeriodMiddle, konsPeriodEnd;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		optifier = new TarmedOptifier();
		coreModelService = AllTestsSuite.getCoreModelService();
		LocalDate now = LocalDate.now();
		IPerson person =
			new IContactBuilder.PersonBuilder(coreModelService, "mandator1 " + now.toString(),
				"Anton" + now.toString(), now, Gender.MALE).mandator().buildAndSave();
		mandator = coreModelService.load(person.getId(), IMandator.class).get();
		
		importTarmedReferenceData();
		
		// init some basic services
		tlBaseFirst5Min = TarmedLeistung.getFromCode("00.0010", LocalDate.now(), null);
		tlBaseXRay = TarmedLeistung.getFromCode("39.0020", LocalDate.now(), null);
		tlBaseRadiologyHospital = TarmedLeistung.getFromCode("39.0015", LocalDate.now(), null);
		tlUltrasound = TarmedLeistung.getFromCode("39.3005", LocalDate.now(), null);
		
		tlAgeTo1Month = TarmedLeistung.getFromCode("00.0870", LocalDate.now(), null);
		tlAgeTo7Years = TarmedLeistung.getFromCode("00.0900", LocalDate.now(), null);
		tlAgeFrom7Years = TarmedLeistung.getFromCode("00.0890", LocalDate.now(), null);
		
		tlGroupLimit1 = TarmedLeistung.getFromCode("02.0310", LocalDate.now(), null);
		tlGroupLimit2 = TarmedLeistung.getFromCode("02.0340", LocalDate.now(), null);
		
		tlAlZero = TarmedLeistung.getFromCode("00.0716", LocalDate.now(), null);
		
		//Patient Grissemann with case and consultation
		patGrissemann = new IContactBuilder.PatientBuilder(coreModelService, "Grissemann",
			"Christoph", LocalDate.of(1966, 05, 17), Gender.MALE).buildAndSave();
		ICoverage fallGriss = new ICoverageBuilder(coreModelService, patGrissemann,
			"Testfall Grissemann", "Krankheit", "KVG").buildAndSave();
		//		fallGriss.setInfoElement("Kostenträger", patGrissemann.getId());
		konsGriss = new IEncounterBuilder(coreModelService, fallGriss, mandator).buildAndSave();
		resetKons(konsGriss);
		
		//Patient Stermann with case and consultation
		patStermann = new IContactBuilder.PatientBuilder(coreModelService, "Stermann", "Dirk",
			LocalDate.of(1965, 7, 12), Gender.MALE).buildAndSave();
		ICoverage fallSter = new ICoverageBuilder(coreModelService, patStermann,
			"Testfall Stermann", "Krankheit", "KVG").buildAndSave();
		//		fallSter.setInfoElement("Kostenträger", patStermann.getId());
		konsSter = new IEncounterBuilder(coreModelService, fallSter, mandator).buildAndSave();
		resetKons(konsSter);
		
		//Patient OneYear with case and consultation
		LocalDate dob = LocalDate.now().minusYears(1).minusDays(1);
		patOneYear =
			new IContactBuilder.PatientBuilder(coreModelService, "One", "Year", dob, Gender.MALE)
				.buildAndSave();
		ICoverage fallOneYear =
			new ICoverageBuilder(coreModelService, patOneYear, "Testfall One", "Krankheit", "KVG")
				.buildAndSave();
		//		fallOneYear.setInfoElement("Kostenträger", patOneYear.getId());
		konsOneYear = new IEncounterBuilder(coreModelService, fallOneYear, mandator).buildAndSave();
		resetKons(konsOneYear);
		
		//Patient below75 with case and consultation
		dob = LocalDate.now().minusYears(74).minusDays(350);
		patBelow75 =
			new IContactBuilder.PatientBuilder(coreModelService, "One", "Year", dob, Gender.MALE)
				.buildAndSave();
		ICoverage fallBelow75 = new ICoverageBuilder(coreModelService, patBelow75,
			"Testfall below 75", "Krankheit", "KVG").buildAndSave();
		//		fallBelow75.setCostBearer(patBelow75);
		konsBelow75 = new IEncounterBuilder(coreModelService, fallBelow75, mandator).buildAndSave();
		resetKons(konsBelow75);
		
		konsPeriodStart = new IEncounterBuilder(coreModelService, fallBelow75, mandator)
			.date(new TimeTool("01.01.2018").toLocalDate()).buildAndSave();
		resetKons(konsPeriodStart);
		
		konsPeriodMiddle = new IEncounterBuilder(coreModelService, fallBelow75, mandator)
			.date(new TimeTool("28.03.2018").toLocalDate()).buildAndSave();
		resetKons(konsPeriodMiddle);
		
		konsPeriodEnd = new IEncounterBuilder(coreModelService, fallBelow75, mandator)
			.date(new TimeTool("02.04.2018").toLocalDate()).buildAndSave();
		resetKons(konsPeriodEnd);
	}
	
	private static void importTarmedReferenceData() throws FileNotFoundException{
		// Importer not provided we import the raw db; set values that would have
		//		// been set by the importer
		OsgiServiceUtil.getService(IConfigService.class).get()
			.set(TarmedReferenceDataImporter.CFG_REFERENCEINFO_AVAILABLE, true);
		//		File tarmedFile = new File(System.getProperty("user.dir") + File.separator + "rsc"
		//			+ File.separator + "tarmed.mdb");
		//		InputStream tarmedInStream = new FileInputStream(tarmedFile);
		//		
		//		TarmedReferenceDataImporter importer = new TarmedReferenceDataImporter();
		//		importer.suppressRestartDialog();
		//		Status retStatus =
		//			(Status) importer.performImport(new NullProgressMonitor(), tarmedInStream, null);
		//		assertEquals(IStatus.OK, retStatus.getCode());
	}
	
	private TarmedLeistung additionalService;
	private TarmedLeistung mainService;
	
	@Test
	public void mutipleBaseFirst5MinIsInvalid(){
		clearKons(konsGriss);
		Result<IBilled> resultGriss = optifier.add(tlBaseFirst5Min, konsGriss);
		assertTrue(resultGriss.isOK());
		resultGriss = optifier.add(tlBaseFirst5Min, konsGriss);
		assertFalse(resultGriss.isOK());
	}
	
	@Test
	public void testAddCompatibleAndIncompatible(){
		clearKons(konsGriss);
		Result<IBilled> resultGriss =
			optifier.add(TarmedLeistung.getFromCode("39.3005", LocalDate.now(), null), konsGriss);
		assertTrue(resultGriss.isOK());
		resultGriss =
			optifier.add(TarmedLeistung.getFromCode("39.0020", LocalDate.now(), null), konsGriss);
		assertFalse(resultGriss.isOK());
		resultGriss =
			optifier.add(TarmedLeistung.getFromCode("01.0110", LocalDate.now(), null), konsGriss);
		assertTrue(resultGriss.isOK());
		resultGriss =
			optifier.add(TarmedLeistung.getFromCode("39.3830", LocalDate.now(), null), konsGriss);
		assertTrue(resultGriss.isOK());
		resetKons(konsGriss);
	}
	
	@Test
	public void testAddMultipleIncompatible(){
		Result<IBilled> resultSter = optifier.add(tlBaseXRay, konsSter);
		assertTrue(resultSter.toString(), resultSter.isOK());
		resultSter = optifier.add(tlUltrasound, konsSter);
		assertFalse(resultSter.isOK());
		resultSter = optifier.add(tlBaseRadiologyHospital, konsSter);
		assertFalse(resultSter.isOK());
	}
	
	@Test
	public void testIsCompatible(){
		Result<IBilled> resCompatible = optifier.isCompatible(tlBaseXRay, tlUltrasound, konsSter);
		assertFalse(resCompatible.isOK());
		String resText = "";
		if (!resCompatible.getMessages().isEmpty()) {
			resText = resCompatible.getMessages().get(0).getText();
		}
		assertEquals("39.3005 nicht kombinierbar mit Kapitel 39.01", resText);
		resCompatible = optifier.isCompatible(tlUltrasound, tlBaseXRay, konsSter);
		assertTrue(resCompatible.isOK());
		
		resCompatible = optifier.isCompatible(tlBaseXRay, tlBaseRadiologyHospital, konsSter);
		assertFalse(resCompatible.isOK());
		if (!resCompatible.getMessages().isEmpty()) {
			resText = resCompatible.getMessages().get(0).getText();
		}
		assertEquals("39.0015 nicht kombinierbar mit Leistung 39.0020", resText);
		
		resCompatible = optifier.isCompatible(tlBaseRadiologyHospital, tlUltrasound, konsSter);
		assertFalse(resCompatible.isOK());
		
		resCompatible = optifier.isCompatible(tlBaseXRay, tlBaseFirst5Min, konsSter);
		assertTrue(resCompatible.isOK());
		
		resCompatible = optifier.isCompatible(tlBaseFirst5Min, tlBaseRadiologyHospital, konsSter);
		assertTrue(resCompatible.isOK());
		
		clearKons(konsSter);
		resCompatible = optifier.isCompatible(tlBaseFirst5Min,
			TarmedLeistung.getFromCode("00.1345", LocalDate.now(), null), konsSter);
		assertFalse(resCompatible.isOK());
		resText = "";
		if (!resCompatible.getMessages().isEmpty()) {
			resText = resCompatible.getMessages().get(0).getText();
		}
		assertEquals("00.1345 nicht kombinierbar mit 00.0010, wegen Block Kumulation", resText);
		
		resCompatible =
			optifier.isCompatible(TarmedLeistung.getFromCode("01.0265", LocalDate.now(), null),
				TarmedLeistung.getFromCode("00.1345", LocalDate.now(), null), konsSter);
		assertTrue(resCompatible.isOK());
		
		resCompatible =
			optifier.isCompatible(TarmedLeistung.getFromCode("00.0510", LocalDate.now(), null),
				TarmedLeistung.getFromCode("03.0020", LocalDate.now(), null), konsSter);
		assertFalse(resCompatible.isOK());
		resText = "";
		if (!resCompatible.getMessages().isEmpty()) {
			resText = resCompatible.getMessages().get(0).getText();
		}
		assertEquals("03.0020 nicht kombinierbar mit 00.0510, wegen Block Kumulation", resText);
		
		resCompatible =
			optifier.isCompatible(TarmedLeistung.getFromCode("00.2510", LocalDate.now(), null),
				TarmedLeistung.getFromCode("03.0020", LocalDate.now(), null), konsSter);
		assertTrue(resCompatible.isOK());
		
		resetKons(konsSter);
	}
	
	@Test
	public void testSetBezug(){
		clearKons(konsSter);
		
		additionalService = TarmedLeistung.getFromCode("39.5010", LocalDate.now(), null);
		mainService = TarmedLeistung.getFromCode("39.5060", LocalDate.now(), null);
		// additional without main, not allowed
		Result<IBilled> resultSter = optifier.add(additionalService, konsSter);
		assertFalse(resultSter.isOK());
		// additional after main, allowed
		resultSter = optifier.add(mainService, konsSter);
		assertTrue(resultSter.isOK());
		assertTrue(getVerrechent(konsSter, mainService).isPresent());
		
		resultSter = optifier.add(additionalService, konsSter);
		assertTrue(resultSter.isOK());
		assertTrue(getVerrechent(konsSter, additionalService).isPresent());
		
		// another additional, not allowed
		resultSter = optifier.add(additionalService, konsSter);
		assertFalse(resultSter.toString(), resultSter.isOK());
		assertTrue(getVerrechent(konsSter, additionalService).isPresent());
		
		// remove, and add again
		Optional<IBilled> verrechnet = getVerrechent(konsSter, additionalService);
		assertTrue(verrechnet.isPresent());
		Result<IBilled> result = optifier.remove(verrechnet.get(), konsSter);
		assertTrue(result.isOK());
		resultSter = optifier.add(additionalService, konsSter);
		assertTrue(resultSter.isOK());
		// add another main and additional
		resultSter = optifier.add(mainService, konsSter);
		assertTrue(resultSter.isOK());
		assertTrue(getVerrechent(konsSter, mainService).isPresent());
		
		resultSter = optifier.add(additionalService, konsSter);
		assertTrue(resultSter.isOK());
		assertTrue(getVerrechent(konsSter, additionalService).isPresent());
		
		// remove main service, should also remove additional service
		verrechnet = getVerrechent(konsSter, mainService);
		result = optifier.remove(verrechnet.get(), konsSter);
		assertTrue(result.isOK());
		assertFalse(getVerrechent(konsSter, mainService).isPresent());
		assertFalse(getVerrechent(konsSter, additionalService).isPresent());
		
		resetKons(konsSter);
	}
	
	@Test
	public void testOneYear(){
		Result<IBilled> result = optifier.add(tlAgeTo1Month, konsOneYear);
		assertFalse(result.isOK());
		
		result = optifier.add(tlAgeTo7Years, konsOneYear);
		assertTrue(result.isOK());
		
		result = optifier.add(tlAgeFrom7Years, konsOneYear);
		assertFalse(result.isOK());
	}
	
	@Test
	public void testBelow75(){
		TarmedLeistung tl = TarmedLeistung.getFromCode("00.0020", LocalDate.now(), null);
		// add age restriction to 75 years with 0 tolerance, for the test, like in tarmed 1.09
		Map<String, String> ext = tl.getExtension().getLimits();
		String origAgeLimits = ext.get(TarmedConstants.TarmedLeistung.EXT_FLD_SERVICE_AGE);
		ext.put(TarmedConstants.TarmedLeistung.EXT_FLD_SERVICE_AGE,
			origAgeLimits + (origAgeLimits.isEmpty() ? "-1|0|75|0|26[2006-04-01|2199-12-31]"
					: ", -1|0|75|0|26[2006-04-01|2199-12-31]"));
		
		TarmedExtension te = (TarmedExtension) tl.getExtension();
		te.setLimits(ext);
		
		Result<IBilled> result = optifier.add(tl, konsBelow75);
		assertTrue(result.isOK());
		resetKons(konsBelow75);
		
		ext.put(TarmedConstants.TarmedLeistung.EXT_FLD_SERVICE_AGE, origAgeLimits);
		te.setLimits(ext);
	}
	
	@Test
	public void testGroupLimitation(){
		// limit on group 31 is 48 times per week
		resetKons(konsGriss);
		for (int i = 0; i < 24; i++) {
			Result<IBilled> result = billSingle(konsGriss, tlGroupLimit1);
			assertTrue(result.isOK());
		}
		assertEquals(2, konsGriss.getBilled().size());
		
		resetKons(konsSter);
		for (int i = 0; i < 24; i++) {
			Result<IBilled> result = billSingle(konsSter, tlGroupLimit2);
			assertTrue(result.isOK());
		}
		assertEquals(2, konsSter.getBilled().size());
		
		Result<IBilled> result = billSingle(konsGriss, tlGroupLimit2);
		assertTrue(result.isOK());
		
		result = billSingle(konsSter, tlGroupLimit1);
		assertTrue(result.isOK());
		
		for (int i = 0; i < 23; i++) {
			result = billSingle(konsGriss, tlGroupLimit1);
			assertTrue(result.isOK());
		}
		for (int i = 0; i < 23; i++) {
			result = billSingle(konsSter, tlGroupLimit2);
			assertTrue(result.isOK());
		}
		
		result = billSingle(konsGriss, tlGroupLimit2);
		assertFalse(result.isOK());
		
		result = billSingle(konsSter, tlGroupLimit1);
		assertFalse(result.isOK());
		
		resetKons(konsGriss);
		resetKons(konsSter);
	}
	
	private Result<IBilled> billSingle(IEncounter encounter, TarmedLeistung billable){
		return AllTestsSuite.getBillingService().bill(billable, encounter, 1);
	}
	
	private static void resetKons(IEncounter kons){
		clearKons(kons);
		kons.addDiagnosis(TessinerCode.getFromCode("T1").get());
		Result<IBilled> result = optifier.add(tlBaseFirst5Min, kons);
		assertTrue(result.isOK());
	}
	
	//	@Test
	//	public void testDignitaet(){
	//		IEncounter kons = konsGriss;
	//		setUpDignitaet(kons);
	//		
	//		// default mandant type is specialist
	//		clearKons(kons);
	//		Result<IBilled> result = kons.addLeistung(tlBaseFirst5Min);
	//		assertTrue(result.isOK());
	//		Verrechnet verrechnet = kons.getVerrechnet(tlBaseFirst5Min);
	//		assertNotNull(verrechnet);
	//		int amountAL = TarmedLeistung.getAL(verrechnet);
	//		assertEquals(1042, amountAL);
	//		Money amount = verrechnet.getNettoPreis();
	//		assertEquals(15.45, amount.getAmount(), 0.01);
	//		
	//		// set the mandant type to practitioner
	//		clearKons(kons);
	//		TarmedLeistung.setMandantType(kons.getMandant(), MandantType.PRACTITIONER);
	//		result = kons.addLeistung(tlBaseFirst5Min);
	//		assertTrue(result.isOK());
	//		verrechnet = kons.getVerrechnet(tlBaseFirst5Min);
	//		assertNotNull(verrechnet);
	//		amountAL = TarmedLeistung.getAL(verrechnet);
	//		assertEquals(969, amountAL);
	//		amount = verrechnet.getNettoPreis();
	//		assertEquals(14.84, amount.getAmount(), 0.01); // 10.42 * 0.83 * 0.93 + 8.19 * 0.83
	//		String alScalingFactor = verrechnet.getDetail("AL_SCALINGFACTOR");
	//		assertEquals("0.93", alScalingFactor);
	//		String alNotScaled = verrechnet.getDetail("AL_NOTSCALED");
	//		assertEquals("1042", alNotScaled);
	//		
	//		result = kons.addLeistung(tlAlZero);
	//		assertTrue(result.isOK());
	//		verrechnet = kons.getVerrechnet(tlAlZero);
	//		assertNotNull(verrechnet);
	//		amountAL = TarmedLeistung.getAL(verrechnet);
	//		assertEquals(0, amountAL);
	//		amount = verrechnet.getNettoPreis();
	//		assertEquals(4.08, amount.getAmount(), 0.01); // 0.0 * 0.83 * 0.93 + 4.92 * 0.83
	//		alScalingFactor = verrechnet.getDetail("AL_SCALINGFACTOR");
	//		assertEquals("0.93", alScalingFactor);
	//		
	//		tearDownDignitaet(kons);
	//		
	//		// set the mandant type to specialist
	//		clearKons(kons);
	//		TarmedLeistung.setMandantType(kons.getMandant(), MandantType.SPECIALIST);
	//		result = kons.addLeistung(tlBaseFirst5Min);
	//		assertTrue(result.isOK());
	//		verrechnet = kons.getVerrechnet(tlBaseFirst5Min);
	//		assertNotNull(verrechnet);
	//		amountAL = TarmedLeistung.getAL(verrechnet);
	//		assertEquals(957, amountAL);
	//		amount = verrechnet.getNettoPreis();
	//		assertEquals(17.76, amount.getAmount(), 0.01);
	//	}
	
	/**
	 * Test combination of session limit with coverage limit.
	 */
	@Test
	public void test9533(){
		clearKons(konsGriss);
		
		Result<IBilled> result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("02.0010", LocalDate.now(), null),
			konsGriss);
		assertTrue(result.isOK());
		
		result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("02.0010", LocalDate.now(), null),
			konsGriss);
		assertTrue(result.isOK());
		
		result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("02.0010", LocalDate.now(), null),
			konsGriss);
		assertTrue(result.isOK());
		
		resetKons(konsGriss);
	}
	
//	@Test
//	public void testIsCompatibleTarmedBilling(){
//		
//		TarmedLeistung tlBaseXRay = (TarmedLeistung) TarmedLeistung.getFromCode("39.0020", null);
//		TarmedLeistung tlUltrasound = (TarmedLeistung) TarmedLeistung.getFromCode("39.3005", null);
//		TarmedLeistung tlBaseRadiologyHospital =
//			(TarmedLeistung) TarmedLeistung.getFromCode("39.0015", null);
//		TarmedLeistung tlBaseFirst5Min =
//			(TarmedLeistung) TarmedLeistung.getFromCode("00.0010", null);
//		
//		IStatus resCompatible =
//			optifier.isCompatible(tlBaseXRay, tlUltrasound, testBehandlungen.get(0));
//		assertFalse(resCompatible.isOK());
//		String resText = "";
//		if (!resCompatible.getMessage().isEmpty()) {
//			resText = resCompatible.getMessage();
//		}
//		assertEquals("39.3005 nicht kombinierbar mit Kapitel 39.01", resText);
//		resCompatible = optifier.isCompatible(tlUltrasound, tlBaseXRay, testBehandlungen.get(0));
//		assertTrue(resCompatible.isOK());
//		
//		resCompatible =
//			optifier.isCompatible(tlBaseXRay, tlBaseRadiologyHospital, testBehandlungen.get(0));
//		assertFalse(resCompatible.isOK());
//		if (!resCompatible.getMessage().isEmpty()) {
//			resText = resCompatible.getMessage();
//		}
//		assertEquals("39.0015 nicht kombinierbar mit Leistung 39.0020", resText);
//		
//		resCompatible =
//			optifier.isCompatible(tlBaseRadiologyHospital, tlUltrasound, testBehandlungen.get(0));
//		assertFalse(resCompatible.isOK());
//		
//		resCompatible = optifier.isCompatible(tlBaseXRay, tlBaseFirst5Min, testBehandlungen.get(0));
//		assertTrue(resCompatible.isOK());
//		
//		resCompatible = optifier.isCompatible(tlBaseFirst5Min, tlBaseRadiologyHospital,
//			testBehandlungen.get(0));
//		assertTrue(resCompatible.isOK());
//	}
	
	/**
	 * Test exclusion with side.
	 */
	@Test
	public void testSideExclusion(){
		clearKons(konsGriss);
		
		Result<IBilled> result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("09.0930", LocalDate.now(), null),
			konsGriss);
		assertTrue(result.isOK());
		
		result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("09.0950", LocalDate.now(), null),
			konsGriss);
		assertFalse(result.isOK());
		assertEquals(TarmedOptifier.EXKLUSIONSIDE, result.getCode());
		
		optifier.putContext(TarmedConstants.TarmedLeistung.SIDE,
			TarmedConstants.TarmedLeistung.SIDE_L);
		result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("09.0950", LocalDate.now(), null),
			konsGriss);
		assertFalse(result.isOK());
		assertEquals(TarmedOptifier.EXKLUSIONSIDE, result.getCode());
		
		optifier.putContext(TarmedConstants.TarmedLeistung.SIDE,
			TarmedConstants.TarmedLeistung.SIDE_R);
		result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("09.0950", LocalDate.now(), null),
			konsGriss);
		assertTrue(result.isOK());
		
		resetKons(konsGriss);
	}
	
	/**
	 * Test limit with side.
	 */
	@Test
	public void testSideLimit(){
		clearKons(konsGriss);
		
		Result<IBilled> result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("39.3408", LocalDate.now(), null),
			konsGriss);
		assertTrue(result.isOK());
		assertEquals(1, getLeistungAmount("39.3408", konsGriss));
		
		result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("39.3408", LocalDate.now(), null),
			konsGriss);
		assertTrue(result.isOK());
		assertEquals(2, getLeistungAmount("39.3408", konsGriss));
		
		Set<String> sides = new HashSet<>();
		List<IBilled> leistungen = getLeistungen("39.3408", konsGriss);
		for (IBilled verrechnet : leistungen) {
			sides.add(TarmedLeistung.getSide(verrechnet));
		}
		assertEquals(2, sides.size());
		assertTrue(sides.contains(TarmedConstants.TarmedLeistung.LEFT));
		assertTrue(sides.contains(TarmedConstants.TarmedLeistung.RIGHT));
		
		result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("39.3408", LocalDate.now(), null),
			konsGriss);
		assertFalse(result.isOK());
		assertEquals(2, getLeistungAmount("39.3408", konsGriss));
		
		resetKons(konsGriss);
	}
	
	/**
	 * Test cleanup after kumulation warning.
	 */
	@Test
	public void testCleanUpAfterKumulation(){
		clearKons(konsGriss);
		
		Result<IBilled> result;
		for (int i = 0; i < 6; i++) {
			result = optifier.add(TarmedLeistung.getFromCode("00.0050", LocalDate.now(), null),
				konsGriss);
			assertTrue(result.isOK());
		}
		result =
			optifier.add(TarmedLeistung.getFromCode("00.0050", LocalDate.now(), null), konsGriss);
		assertFalse(result.isOK());
		assertEquals(6, konsGriss.getBilled().get(0).getAmount(), 0.01);
		
		clearKons(konsGriss);
		result = optifier.add(tlBaseFirst5Min, konsGriss);
		assertTrue(result.isOK());
		result =
			optifier.add(TarmedLeistung.getFromCode("00.0020", LocalDate.now(), null), konsGriss);
		assertTrue(result.isOK());
		result =
			optifier.add(TarmedLeistung.getFromCode("00.0020", LocalDate.now(), null), konsGriss);
		assertTrue(result.isOK());
		result =
			optifier.add(TarmedLeistung.getFromCode("00.0030", LocalDate.now(), null), konsGriss);
		assertTrue(result.isOK());
		result = optifier.add(tlBaseFirst5Min, konsGriss);
		assertFalse(result.isOK());
		assertEquals(1, getLeistungAmount("00.0010", konsGriss));
		result =
			optifier.add(TarmedLeistung.getFromCode("00.0020", LocalDate.now(), null), konsGriss);
		assertFalse(result.isOK());
		assertEquals(2, getLeistungAmount("00.0020", konsGriss));
		
		resetKons(konsGriss);
	}
	
	@Test
	public void testKumulationSide(){
		clearKons(konsGriss);
		
		Result<IBilled> result = optifier.add(
			TarmedLeistung.getFromCode("20.0330", LocalDate.now(), null),
			konsGriss);
		assertTrue(result.isOK());
		result = optifier.add(
			TarmedLeistung.getFromCode("20.0330", LocalDate.now(), null),
			konsGriss);
		assertFalse(result.toString(), result.isOK());
		
		clearKons(konsGriss);
	}
	
	/**
	 * Test of limitation per period currently Tarmed 1.08 00.0140 limit 10 per 3 month. <br />
	 * With Tarmed 1.09 00.0141 should be used limit 30 per 3 month.
	 * 
	 */
	@Test
	public void testLimitationPeriod(){
		clearKons(konsPeriodStart);
		clearKons(konsPeriodMiddle);
		clearKons(konsPeriodEnd);
		Result<IBilled> result = null;
		// start and middle are 1 period
		for (int i = 0; i < 6; i++) {
			result = optifier.add(
				(TarmedLeistung) TarmedLeistung.getFromCode("00.0140", LocalDate.now(), null),
				konsPeriodStart);
			assertTrue(result.isOK());
		}
		for (int i = 0; i < 6; i++) {
			result = optifier.add(
				(TarmedLeistung) TarmedLeistung.getFromCode("00.0140", LocalDate.now(), null),
				konsPeriodMiddle);
			assertTrue(result.isOK());
		}
		result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("00.0140", LocalDate.now(), null),
			konsPeriodMiddle);
		assertFalse(result.isOK());
		// end is after period so middle is not included for limit
		for (int i = 0; i < 7; i++) {
			result = optifier.add(
				(TarmedLeistung) TarmedLeistung.getFromCode("00.0140", LocalDate.now(), null),
				konsPeriodEnd);
			assertTrue(result.isOK());
		}
		
		clearKons(konsPeriodStart);
		clearKons(konsPeriodMiddle);
		clearKons(konsPeriodEnd);
	}
	
	@Test
	public void testAdditionalBlockExclusion(){
		clearKons(konsGriss);
		
		Result<IBilled> result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("17.0710", LocalDate.now(), null),
			konsGriss);
		assertTrue(result.isOK());
		result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("17.0740", LocalDate.now(), null),
			konsGriss);
		assertTrue(result.isOK());
		// additional service, not in block LB-05, billing is allowed anyway
		result = optifier.add(
			(TarmedLeistung) TarmedLeistung.getFromCode("17.0750", LocalDate.now(), null),
			konsGriss);
		assertTrue(result.isOK());
		
		clearKons(konsGriss);
	}
	
	private int getLeistungAmount(String code, IEncounter kons){
		int ret = 0;
		for (IBilled leistung : kons.getBilled()) {
			if (leistung.getBillable().getCode().equals(code)) {
				ret += leistung.getAmount();
			}
		}
		return ret;
	}
	
	private List<IBilled> getLeistungen(String code, IEncounter kons){
		List<IBilled> ret = new ArrayList<>();
		for (IBilled leistung : kons.getBilled()) {
			if (leistung.getBillable().getCode().equals(code)) {
				ret.add(leistung);
			}
		}
		return ret;
	}
	
	//	private void setUpDignitaet(IEncounter kons){
	//		Map<String, String> extension = tlBaseFirst5Min.getExtension().getLimits();
	//		// set reduce factor
	//		extension.put(TarmedConstants.TarmedLeistung.EXT_FLD_F_AL_R, "0.93");
	//		// the AL value
	//		extension.put(TarmedConstants.TarmedLeistung.EXT_FLD_TP_AL, "10.42");
	//		tlBaseFirst5Min.setExtension(extension);
	//		extension = tlAlZero.loadExtension();
	//		// set reduce factor
	//		extension.put(TarmedConstants.TarmedLeistung.EXT_FLD_F_AL_R, "0.93");
	//		// no AL value
	//		tlAlZero.setExtension(extension);
	//		
	//		// add additional multiplier
	//		LocalDate yesterday = LocalDate.now().minus(1, ChronoUnit.DAYS);
	//		MultiplikatorList multis =
	//			new MultiplikatorList("VK_PREISE", kons.getFall().getAbrechnungsSystem());
	//		multis.insertMultiplikator(new TimeTool(yesterday), "0.83");
	//		
	//	}
	
	//	private void tearDownDignitaet(Konsultation kons){
	//		Hashtable<String, String> extension = tlBaseFirst5Min.loadExtension();
	//		// clear reduce factor
	//		extension = tlBaseFirst5Min.loadExtension();
	//		extension.remove(TarmedLeistung.EXT_FLD_F_AL_R);
	//		// reset AL value
	//		extension.put(TarmedLeistung.EXT_FLD_TP_AL, "9.57");
	//		tlBaseFirst5Min.setExtension(extension);
	//		extension = tlAlZero.loadExtension();
	//		// clear reduce factor
	//		extension.remove(TarmedLeistung.EXT_FLD_F_AL_R);
	//		// no AL value
	//		tlAlZero.setExtension(extension);
	//		
	//		// remove additional multiplier
	//		LocalDate yesterday = LocalDate.now().minus(1, ChronoUnit.DAYS);
	//		MultiplikatorList multis =
	//			new MultiplikatorList("VK_PREISE", kons.getFall().getAbrechnungsSystem());
	//		multis.removeMultiplikator(new TimeTool(yesterday), "0.83");
	//		
	//	}
	//	
	private static void clearKons(IEncounter kons){
		for (IBilled verrechnet : kons.getBilled()) {
			coreModelService.remove(verrechnet);
		}
	}
	
	private Optional<IBilled> getVerrechent(IEncounter kons, TarmedLeistung leistung){
		for (IBilled verrechnet : kons.getBilled()) {
			if (verrechnet.getBillable().getCode().equals(leistung.getCode())) {
				return Optional.of(verrechnet);
			}
		}
		return Optional.empty();
	}
}