package ucu.mis.services.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import ucu.mis.arims.model.ArimsStudentRegistration;
import ucu.mis.arims.model.ArimsUACEResult;
import ucu.mis.arims.model.ArimsUCEResult;
import ucu.mis.arims.repository.ArimsStudentRegistrationRepository;
import ucu.mis.arims.repository.ArimsStudentSemesterRegistrationRepository;
import ucu.mis.arims.repository.ArimsUACEResultRepository;
import ucu.mis.arims.repository.ArimsUCEResultRepository;
import ucu.mis.model.acd.Applicant;
import ucu.mis.model.acd.Program;
import ucu.mis.model.acd.ProgramCore;
import ucu.mis.model.acd.Student;
import ucu.mis.payload.arims.ArimsSubjectRequest;
import ucu.mis.payload.reports.StudentDataProfilesPayLoad;
import ucu.mis.profiles.model.FilteredCompleteStudentProfiles;
import ucu.mis.profiles.model.ProgramMappings;
import ucu.mis.profiles.model.RefinedStudentProfiles;
import ucu.mis.profiles.model.StudentApplicantProfile;
import ucu.mis.profiles.model.StudentArimsProfiles;
import ucu.mis.profiles.payload.PredictionInsightRequest;
import ucu.mis.profiles.payload.PredictionInsights;
import ucu.mis.profiles.payload.RefinedCgpaPrediction;
import ucu.mis.profiles.repository.FilteredCompleteStudentProfilesRepository;
import ucu.mis.profiles.repository.ProgramMappingsRepository;
import ucu.mis.profiles.repository.RefinedStudentProfilesRepository;
import ucu.mis.profiles.repository.StudentApplicantProfileRepository;
import ucu.mis.profiles.repository.StudentArimsProfilesRepository;
import ucu.mis.repository.acd.ProgramCoreRepository;
import ucu.mis.repository.acd.ProgramRepository;
import ucu.mis.repository.acd.StudentRepository;
import ucu.mis.services.arims.ArimsMappingService;
import ucu.mis.services.general.GeneralService;
import ucu.mis.util.enums.Nationality;
import ucu.mis.util.enums.ProgramLevel;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class StudentDataProfilesService {

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private ArimsStudentRegistrationRepository arimsStudentRegistrationRepository;

	@Autowired
	private ArimsStudentSemesterRegistrationRepository arimsStudentSemesterRegistrationRepository;

	@Autowired
	private StudentApplicantProfileRepository studentApplicantProfileRepository;

	@Autowired
	private StudentArimsProfilesRepository studentArimsProfilesRepository;

	@Autowired
	private ArimsUCEResultRepository arimsUCEResultRepository;

	@Autowired
	private RefinedStudentProfilesRepository refinedStudentProfilesRepository;

	@Autowired
	private FilteredCompleteStudentProfilesRepository filteredCompleteStudentProfilesRepository;

	@Autowired
	private ArimsUACEResultRepository arimsUACEResultRepository;

	@Autowired
	private ArimsMappingService arimsMappingService;

	@Autowired
	private GeneralService generalService;

	@Autowired
	private ProgramCoreRepository programCoreRepository;

	@Autowired
	private ProgramRepository programRepository;

	@Autowired
	private ProgramMappingsRepository programMappingsRepository;

	public ArrayList<StudentDataProfilesPayLoad> generateStudentDataProfiles() {

		List<Student> students = studentRepository.findAll();

		System.out.println("students: " + students.size());

		// A77051
		int arims = 0;
		int applicants = 0;

		for (Student student : students) {

			// Connection and Presence in ARIMS
			Optional<ArimsStudentRegistration> arimsOpt = student.getAccessNo() != null
					? arimsStudentRegistrationRepository.findByAccessNumber(student.getAccessNo())
					: Optional.empty();

			// First Application system
			Applicant applicant = student.getApplicant() != null ? student.getApplicant() : null;

			if (arimsOpt.isPresent()) {

				StudentApplicantProfile profile = saveApplicantProfile(student, "arims");

				String referenceNumber = arimsOpt.get().getRefNumber();

				ArimsSubjectRequest arimsDetails = arimsMappingService.getApplicantArimsGrades(referenceNumber);

				profile.setFirstChoice(arimsDetails.getFirstChoice());
				profile.setSecondChoice(arimsDetails.getSecondChoice());
				profile.setAdmissionProgram(arimsDetails.getAdmissionCourse());
				profile.setApplicationDate(arimsDetails.getApplicationDate());

				Optional<ArimsStudentRegistration> arimsStudent = arimsStudentRegistrationRepository
						.findByRefNumber(referenceNumber);

				if (arimsStudent.isPresent()) {

					profile.setAcademicYear(arimsStudent.get().getAcademicYear());
					profile.setCalendarYear(
							arimsStudent.get().getCalendarYear() != null ? (long) arimsStudent.get().getCalendarYear()
									: null);

					profile.setUceCenter(arimsStudent.get().getUceCenter());
					profile.setUceCredits((long) arimsStudent.get().getUceCredits());
					profile.setUceDinstinctions((long) arimsStudent.get().getUceDinstinctions());
					profile.setUceIndexNumber(arimsStudent.get().getUceIndexNumber());
					profile.setUceYear(arimsStudent.get().getUceYear());

					profile.setUaceCenter(arimsStudent.get().getUaceCenter());
					profile.setUaceIndexNumber(arimsStudent.get().getUaceIndexNumber());
					profile.setUaceYear(arimsStudent.get().getUaceYear());
					profile.setGeneralPaper((long) arimsStudent.get().getGeneralPaper());
					profile.setSubjectCombination(arimsStudent.get().getSubjectCombination());

				}

				if (studentApplicantProfileRepository.save(profile) != null && arimsStudent.isPresent()) {

					// Save Grades
					saveOLevelResults(arimsStudent.get(), profile);

				}

				if (profile != null) {
					arims += 1;
				}

			}

			else if (applicant != null) {

				StudentApplicantProfile profile = saveApplicantProfile(student, "alpha");

				if (profile != null) {
					applicants += 1;
				}

			} else {
				System.out.println("Student Applicant not found");
			}

		}

		System.out.println("Arims Students: " + arims);
		System.out.println("1st Application: " + applicants);

		return null;
	}

	private StudentApplicantProfile saveApplicantProfile(Student student, String source) {

		if (student != null) {
			Optional<StudentApplicantProfile> savedProfile = studentApplicantProfileRepository
					.findByAccessNo(student.getAccessNo());

			savedProfile = !savedProfile.isPresent()
					? studentApplicantProfileRepository.findByStudentId(student.getId())
					: savedProfile;

			StudentApplicantProfile profile = savedProfile.isPresent() ? savedProfile.get()
					: new StudentApplicantProfile();

			profile.setRegno(student.getRegno());
			profile.setAccessNo(student.getAccessNo());
			profile.setStudentId(student.getId());
			profile.setSource(source);
			profile.setCampus(student.getCampus() != null ? student.getCampus().getName().toLowerCase() : null);
			profile.setCampusId(student.getCampus() != null ? student.getCampus().getId() : null);
			profile.setProgram(student.getCurriculum() != null
					? student.getCurriculum().getProgram().getProgramCore().getProgramName()
					: null);
			profile.setProgramId(
					student.getCurriculum() != null ? student.getCurriculum().getProgram().getProgramCore().getId()
							: null);
			profile.setProgramType(student.getCurriculum() != null
					? student.getCurriculum().getProgram().getProgramCore().getProgramLevel().name().toLowerCase()
					: null);
			profile.setStudyYears(student.getCurriculum() != null
					? (long) student.getCurriculum().getProgram().getProgramCore().getMinimum_years()
					: null);
			profile.setYearOfEntry(student.getYearOfEntry() > 0 ? (long) student.getYearOfEntry() : null);
			profile.setNationality(
					student.getNationality() != null ? student.getNationality().name().toLowerCase() : null);
			profile.setTuition(
					student.getFeesStructure() != null ? student.getNationality() != null
							? student.getNationality().equals(ucu.mis.util.enums.Nationality.NATIONAL)
									? student.getFeesStructure().getNational()
									: student.getFeesStructure().getInternational()
							: null : null);
			profile.setStudyStatus(student.getStatus() != null ? student.getStatus().name().toLowerCase() : null);

			profile.setCgpa(student.getCurrentCgpa());

			/*
			 * Profile Attributes
			 */
			profile.setGender(student.getUser().getPerson().getGender() != null
					? student.getUser().getPerson().getGender().name().toLowerCase()
					: null);
			profile.setGenderId(student.getUser().getPerson().getGender() != null
					? (long) student.getUser().getPerson().getGender().getValue()
					: null);
			profile.setDateOfBirth(student.getUser().getPerson().getDate_of_birth());
			profile.setMaritalStatus(student.getUser().getPerson().getMarital_status() != null
					? student.getUser().getPerson().getMarital_status().name().toLowerCase()
					: null);
			profile.setMaritalStatusId(student.getUser().getPerson().getMarital_status() != null
					? (long) student.getUser().getPerson().getMarital_status().getValue()
					: null);

			profile.setReligion(student.getUser().getPerson().getReligious_affiliation() != null
					? student.getUser().getPerson().getReligious_affiliation().name().toLowerCase()
					: null);
			profile.setReligionId(student.getUser().getPerson().getReligious_affiliation() != null
					? (long) student.getUser().getPerson().getReligious_affiliation().getValue()
					: null);
			profile.setDisabled(student.getUser().getPerson().getDisabilityDescription() != null
					|| (student.getUser().getPerson().getApplicantDisability() != null
							&& student.getUser().getPerson().getApplicantDisability().size() > 0) ? true : false);
			profile.setChildren(student.getUser().getPerson().getNum_of_children() != null
					&& student.getUser().getPerson().getNum_of_children() > 0
							? (long) student.getUser().getPerson().getNum_of_children()
							: null);

			return studentApplicantProfileRepository.save(profile);
		}

		return null;
	}

	public ArrayList<StudentDataProfilesPayLoad> generateArimsStudentDataProfiles() {

		List<ArimsStudentRegistration> students = arimsStudentRegistrationRepository.getAllRegistrations();

		System.out.println("students: " + students.size());

		// A77051
		int arims = students.size();
		int inAlpha = 0;
		int notInAlpha = 0;

		for (ArimsStudentRegistration arimsOpt : students) {

			if (arimsOpt != null) {

				StudentArimsProfiles profile = generateStudentArimsProfile(arimsOpt);

				if (profile != null) {

					if (profile.getIsInAlpha()) {
						inAlpha += 1;
					} else {
						notInAlpha += 1;
					}
				}

			}

			else {
				System.out.println("Student Applicant not found");
			}

		}

		System.out.println("Arims Students: " + arims);
		System.out.println("In Alpha: " + inAlpha);
		System.out.println("Not in Alpha: " + notInAlpha);

		return null;
	}

	public StudentArimsProfiles generateStudentArimsProfile(ArimsStudentRegistration arimsOpt) {

		StudentArimsProfiles profile = saveApplicantProfile(arimsOpt);

		String referenceNumber = arimsOpt.getRefNumber();

		ArimsSubjectRequest arimsDetails = arimsMappingService.getApplicantArimsGrades(referenceNumber);

		profile.setFirstChoice(arimsDetails.getFirstChoice() != null ? arimsDetails.getFirstChoice() : "Unknown");
		profile.setSecondChoice(arimsDetails.getSecondChoice() != null ? arimsDetails.getSecondChoice() : "Unknown");
		profile.setAdmissionProgram(arimsDetails.getAdmissionCourse());
		profile.setApplicationDate(arimsDetails.getApplicationDate());

		Optional<ArimsStudentRegistration> arimsStudent = arimsStudentRegistrationRepository
				.findByRefNumber(referenceNumber);

		if (arimsStudent.isPresent()) {

			profile.setAcademicYear(arimsStudent.get().getAcademicYear());
			profile.setCalendarYear(
					arimsStudent.get().getCalendarYear() != null ? (long) arimsStudent.get().getCalendarYear() : null);

			profile.setUceCenter(arimsStudent.get().getUceCenter());
			profile.setUceCredits((long) arimsStudent.get().getUceCredits());
			profile.setUceDinstinctions((long) arimsStudent.get().getUceDinstinctions());
			profile.setUceIndexNumber(arimsStudent.get().getUceIndexNumber());
			profile.setUceYear(arimsStudent.get().getUceYear());

			profile.setUaceCenter(arimsStudent.get().getUaceCenter());
			profile.setUaceIndexNumber(arimsStudent.get().getUaceIndexNumber());
			profile.setUaceYear(arimsStudent.get().getUaceYear());
			profile.setGeneralPaper((long) arimsStudent.get().getGeneralPaper());
			profile.setSubjectCombination(arimsStudent.get().getSubjectCombination());

		}

		StudentArimsProfiles saved = studentArimsProfilesRepository.save(profile);

		if (saved != null) {

			Optional<Student> alphaStudent = studentRepository.findByAccessNo(saved.getAccessNo());

			if (alphaStudent.isPresent()) {

				Student student = alphaStudent.get();

				saved.setIsInAlpha(true);
				saved.setCgpa(student.getCurrentCgpa());
				saved.setCampusId(student.getCampus() != null ? student.getCampus().getId() : null);
				saved.setProgramId(
						student.getCurriculum() != null ? student.getCurriculum().getProgram().getProgramCore().getId()
								: null);
				saved.setProgramType(student.getCurriculum() != null
						? student.getCurriculum().getProgram().getProgramCore().getProgramLevel().name().toLowerCase()
						: null);
				saved.setStudyYears(student.getCurriculum() != null
						? (long) student.getCurriculum().getProgram().getProgramCore().getMinimum_years()
						: null);
				saved.setYearOfEntry(student.getYearOfEntry() > 0 ? (long) student.getYearOfEntry() : null);
			} else {

				saved.setIsInAlpha(false);
			}

			StudentArimsProfiles updatedStudentArimsProfiles = studentArimsProfilesRepository.save(saved);

			if (updatedStudentArimsProfiles != null) {
				// Save Grades
				saveOLevelResults(arimsStudent.get(), updatedStudentArimsProfiles);

				return updatedStudentArimsProfiles;
			}

		}
		return profile;
	}

	private StudentArimsProfiles saveApplicantProfile(ArimsStudentRegistration student) {

		if (student != null) {

			Optional<StudentArimsProfiles> savedProfile = studentArimsProfilesRepository
					.findByAccessNo(student.getAccessNumber());

			StudentArimsProfiles profile = savedProfile.isPresent() ? savedProfile.get() : new StudentArimsProfiles();

			profile.setRegno(student.getAdminNumber());
			profile.setAccessNo(student.getAccessNumber());
			profile.setCampus(student.getCampus() != null ? student.getCampus().toLowerCase() : null);
			profile.setLevel(student.getLevel());
			profile.setEntryCategory(student.getEntryCategory());
			profile.setStudyTime(student.getStudyTime());
			profile.setStudentStatus(student.getStudentStatus());
			profile.setSession(student.getSession());
			profile.setInternationalStudent(student.getInternationalStudent());
			profile.setNation(student.getNation());
			profile.setTuition(student.getFees() != null ? (double) student.getFees() : null);
			profile.setPreYear(student.getPreYear());
			profile.setAdmissionStatus(student.getAdmissionStatus());

			/*
			 * Profile Attributes
			 */
			profile.setGender(student.getSex());
			profile.setDateOfBirth(student.getDob());
			profile.setMaritalStatus(student.getMaritalStatus());
			profile.setChildren(student.getChildren() != null ? (long) student.getChildren() : null);

			try {
				return studentArimsProfilesRepository.save(profile);
			} catch (Exception e) {
				// TODO: handle exception
			}

		}

		return null;
	}

	public StudentApplicantProfile saveOLevelResults(ArimsStudentRegistration arimsStudent,
			StudentApplicantProfile profle) {

		List<ArimsUCEResult> arimsResults = arimsUCEResultRepository.findByRefNumber(arimsStudent.getRefNumber());

		// Calculate the best sum out of 6 grades
		int bestSumOutOfSix = calculateBestSum(arimsResults, 6);

		// Calculate the best sum out of 8 grades
		int bestSumOutOfEight = calculateBestSum(arimsResults, 8);

		// Calculate the best sum out of 10 grades
		int bestSumOutOfTen = calculateBestSum(arimsResults, 10);

		StudentApplicantProfile savedProfile = profle;

		savedProfile.setOlevelSubjects(arimsResults.size() > 0 ? (long) arimsResults.size() : null);
		savedProfile.setBestSumOutOfSix(arimsResults.size() > 0 ? (long) bestSumOutOfSix : null);
		savedProfile.setBestSumOutOfEight(arimsResults.size() > 0 ? (long) bestSumOutOfEight : null);
		savedProfile.setBestSumOutOfTen(arimsResults.size() > 0 ? (long) bestSumOutOfTen : null);

		return studentApplicantProfileRepository.save(savedProfile);
	}

	public StudentArimsProfiles saveOLevelResults(ArimsStudentRegistration arimsStudent, StudentArimsProfiles profle) {

		List<ArimsUCEResult> arimsResults = arimsUCEResultRepository.findByRefNumber(arimsStudent.getRefNumber());

		// Calculate the best sum out of 6 grades
		int bestSumOutOfSix = calculateBestSum(arimsResults, 6);

		// Calculate the best sum out of 8 grades
		int bestSumOutOfEight = calculateBestSum(arimsResults, 8);

		// Calculate the best sum out of 10 grades
		int bestSumOutOfTen = calculateBestSum(arimsResults, 10);

		StudentArimsProfiles savedProfile = profle;

		savedProfile.setOlevelSubjects(arimsResults.size() > 0 ? (long) arimsResults.size() : null);
		savedProfile.setBestSumOutOfSix(arimsResults.size() > 0 ? (long) bestSumOutOfSix : null);
		savedProfile.setBestSumOutOfEight(arimsResults.size() > 0 ? (long) bestSumOutOfEight : null);
		savedProfile.setBestSumOutOfTen(arimsResults.size() > 0 ? (long) bestSumOutOfTen : null);

		return studentArimsProfilesRepository.save(savedProfile);
	}

	// Method to calculate the best sum out of a list of ArimsUCEResult objects for
	public int calculateBestSum(List<ArimsUCEResult> results, int count) {
		// Sort the grades in ascending order
		List<ArimsUCEResult> sortedResults = results.stream().sorted(Comparator.comparingInt(ArimsUCEResult::getGrade))
				.collect(Collectors.toList());

		int totalSum = 0;
		for (int i = 0; i < count && i < sortedResults.size(); i++) {
			totalSum += sortedResults.get(i).getGrade();
		}
		return totalSum;
	}

	/*
	 * Testing Predictions
	 */
	public PredictionInsights checkStudentDataProfiles(PredictionInsightRequest request) {

		PredictionInsights predictionMapper = new PredictionInsights();

		Student student = studentRepository.findByAccessNo(request.getAccessNumber()).orElse(null);

		if (student != null) {

			// Connection and Presence in ARIMS
			StudentArimsProfiles profile = studentArimsProfilesRepository.findByAccessNo(student.getAccessNo())
					.orElse(null);

			if (profile == null) {

				ArimsStudentRegistration arimsOpt = arimsStudentRegistrationRepository
						.findByAccessNumber(student.getAccessNo()).orElse(null);

				// Save Profile
				profile = arimsOpt != null ? generateStudentArimsProfile(arimsOpt) : profile;

			}

			if (profile != null) {

				JSONObject featurePayload = extractFeaturesFromArimsStudent(profile);

				System.out.println("featurePayload: " + featurePayload);

				String prediction = predictCGPA(featurePayload);

				System.out.println("prediction: " + prediction);

				predictionMapper = predictionMapper(prediction);

			}

		}

		return predictionMapper;

	}

	public String predictCGPA(JSONObject features) {

		// REST Template setup
		RestTemplate restTemplate = new RestTemplate();
		String url = "http://localhost:5100/predict";

		// Set headers for JSON content
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		// Create the JSON payload
		JSONObject payload = new JSONObject();
		payload.put("data", features);

		// Create the HTTP request
		HttpEntity<String> request = new HttpEntity<>(payload.toString(), headers);

		try {
			// Make the POST request
			ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

			// Return the predicted CGPA from the response
			return response.getBody();
		} catch (HttpClientErrorException e) {
			System.out.println("Error during prediction: " + e.getMessage());
			return "Prediction failed";
		}
	}

	public JSONObject extractFeaturesFromArimsStudent(StudentArimsProfiles student) {
		JSONObject featurePayload = new JSONObject();

		// Numerical Features
		featurePayload.put("general_paper", student.getGeneralPaper() != null ? student.getGeneralPaper() : 0);
		featurePayload.put("tuition", student.getTuition() != null ? student.getTuition() : 0);
		featurePayload.put("uce_credits", student.getUceCredits() != null ? student.getUceCredits() : 0);
		featurePayload.put("uce_dinstinctions",
				student.getUceDinstinctions() != null ? student.getUceDinstinctions() : 0);
		featurePayload.put("campus_id", mapCampusToId(student.getCampus()));
		featurePayload.put("study_years", student.getStudyYears() != null ? student.getStudyYears() : 0);
		featurePayload.put("best_sum_out_of_ten",
				student.getBestSumOutOfTen() != null ? student.getBestSumOutOfTen() : 0);
		featurePayload.put("olevel_subjects", student.getOlevelSubjects() != null ? student.getOlevelSubjects() : 0);

		// Calculate age from DOB
		if (student.getDateOfBirth() != null) {
			int currentYear = LocalDate.now().getYear();
			int birthYear = student.getDateOfBirth().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear();
			featurePayload.put("age", currentYear - birthYear);
		} else {
			featurePayload.put("age", 18); // Default age
		}

		// Dynamic Categorical Features
		// Admission Programs
		List<String> admissionPrograms = studentArimsProfilesRepository.findAllUniqueAdmissionPrograms();
		for (String program : admissionPrograms) {
			featurePayload.put("admission_program_" + program,
					program.equalsIgnoreCase(student.getAdmissionProgram()) ? 1 : 0);
		}

		// Campuses
		List<String> campuses = studentArimsProfilesRepository.findAllUniqueCampuses();
		for (String campus : campuses) {
			featurePayload.put("campus_" + campus, campus.equalsIgnoreCase(student.getCampus()) ? 1 : 0);
		}

		// Entry Categories
		List<String> entryCategories = studentArimsProfilesRepository.findAllUniqueEntryCategories();
		for (String category : entryCategories) {
			featurePayload.put("entry_category_" + category,
					category.equalsIgnoreCase(student.getEntryCategory()) ? 1 : 0);
		}

		// Program Types
		List<String> programTypes = studentArimsProfilesRepository.findAllUniqueProgramTypes();
		for (String type : programTypes) {
			featurePayload.put("program_type_" + type, type.equalsIgnoreCase(student.getProgramType()) ? 1 : 0);
		}

		// Marital Status
		List<String> maritalStatuses = studentArimsProfilesRepository.findAllUniqueMaritalStatuses();
		for (String status : maritalStatuses) {
			featurePayload.put("marital_status_" + status, status.equalsIgnoreCase(student.getMaritalStatus()) ? 1 : 0);
		}

		// Nations
		List<String> nations = studentArimsProfilesRepository.findAllUniqueNations();
		for (String nation : nations) {
			featurePayload.put("nation_" + nation, nation.equalsIgnoreCase(student.getNation()) ? 1 : 0);
		}

		// Student Status
		List<String> studentStatuses = studentArimsProfilesRepository.findAllUniqueStudentStatuses();
		for (String status : studentStatuses) {
			featurePayload.put("student_status_" + status, status.equalsIgnoreCase(student.getStudentStatus()) ? 1 : 0);
		}

		// Class of Degree
		List<String> classOfDegrees = Arrays.asList("First Class", "Pass", "Second Class Lower", "Second Class Upper");
		for (String classOfDegree : classOfDegrees) {
			featurePayload.put("class_of_degree_" + classOfDegree, classOfDegree.equalsIgnoreCase("none") ? 1 : 0);
		}

		// Ensure all expected features exist in the payload
		Set<String> expectedFeatures = generateExpectedFeatures(); // A method to get all expected features as a Set
		for (String feature : expectedFeatures) {
			featurePayload.putIfAbsent(feature, 0); // Default to 0 for missing features
		}

		return featurePayload;
	}

// Helper method to generate expected features
	private Set<String> generateExpectedFeatures() {
		Set<String> expectedFeatures = new HashSet<>();

		// Add numerical features
		expectedFeatures.addAll(Arrays.asList("general_paper", "tuition", "uce_credits", "uce_dinstinctions",
				"campus_id", "study_years", "best_sum_out_of_ten", "olevel_subjects", "age"));

		// Add all unique dynamic features
		expectedFeatures.addAll(studentArimsProfilesRepository.findAllUniqueAdmissionPrograms().stream()
				.map(program -> "admission_program_" + program).collect(Collectors.toSet()));

		expectedFeatures.addAll(studentArimsProfilesRepository.findAllUniqueCampuses().stream()
				.map(campus -> "campus_" + campus).collect(Collectors.toSet()));

		expectedFeatures.addAll(studentArimsProfilesRepository.findAllUniqueEntryCategories().stream()
				.map(category -> "entry_category_" + category).collect(Collectors.toSet()));

		expectedFeatures.addAll(studentArimsProfilesRepository.findAllUniqueProgramTypes().stream()
				.map(type -> "program_type_" + type).collect(Collectors.toSet()));

		expectedFeatures.addAll(studentArimsProfilesRepository.findAllUniqueMaritalStatuses().stream()
				.map(status -> "marital_status_" + status).collect(Collectors.toSet()));

		expectedFeatures.addAll(studentArimsProfilesRepository.findAllUniqueNations().stream()
				.map(nation -> "nation_" + nation).collect(Collectors.toSet()));

		expectedFeatures.addAll(studentArimsProfilesRepository.findAllUniqueStudentStatuses().stream()
				.map(status -> "student_status_" + status).collect(Collectors.toSet()));

		expectedFeatures.addAll(Arrays.asList("class_of_degree_First Class", "class_of_degree_Pass",
				"class_of_degree_Second Class Lower", "class_of_degree_Second Class Upper"));

		return expectedFeatures;
	}

// Helper methods to map categorical data to numerical IDs or One-Hot Encodings
	private int mapCampusToId(String campus) {
		switch (campus != null ? campus.toLowerCase() : "") {
		case "main campus":
			return 1;
		case "kampala campus":
			return 2;
		// Add other mappings here
		default:
			return 0;
		}
	}

	private int mapAdmissionProgramToOneHot(String program, String targetProgram) {
		return program != null && program.equalsIgnoreCase(targetProgram) ? 1 : 0;
	}

	private int mapCampusToOneHot(String campus, String targetCampus) {
		return campus != null && campus.equalsIgnoreCase(targetCampus) ? 1 : 0;
	}

	private PredictionInsights predictionMapper(String jsonResponse) {

		ObjectMapper objectMapper = new ObjectMapper();
		try {

			PredictionInsights insights = objectMapper.readValue(jsonResponse, PredictionInsights.class);

			// Access specific insights
//			System.out.println("Benchmark Comparison: " + insights.getInsights().getBenchmarkComparison());
//			System.out.println("Missing Features: " + insights.getInsights().getMissingFeatures());
//			System.out.println("Next Steps: " + insights.getInsights().getNextSteps());
//			System.out.println("Prediction Confidence: " + insights.getInsights().getPredictionConfidence());
//			System.out.println("Sensitivity: " + insights.getInsights().getSensitivity());
//			System.out.println("Similar Predictions: " + insights.getInsights().getSimilarPredictions());
//			System.out.println("Top Features: " + insights.getInsights().getTopFeatures());
//			System.out.println("Timestamp: " + insights.getInsights().getTimestamp());
//			System.out.println("Prediction: " + insights.getPrediction());

			return insights;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	/*
	 * Migration Scripts
	 */
	public void refineStudentData() {
		// Fetch raw student data
		List<StudentArimsProfiles> rawStudents = studentArimsProfilesRepository.findAll();

		System.out.println("rawStudents: " + rawStudents.size());

		// Transform raw data into refined data
		List<RefinedStudentProfiles> refinedStudents = rawStudents.stream().map(this::transformToRefined)
				.collect(Collectors.toList());

		System.out.println("refinedStudents: " + refinedStudents.size());

		// Check and save refined students
		for (RefinedStudentProfiles refined : refinedStudents) {
			// Check if the record exists by accessNo
			Optional<RefinedStudentProfiles> existingRecord = refinedStudentProfilesRepository
					.findByAccessNo(refined.getAccessNo());

			if (existingRecord.isPresent()) {
				// Update existing record
				RefinedStudentProfiles existing = existingRecord.get();

				updateExistingRecord(existing, refined);

				refinedStudentProfilesRepository.save(existing);
			} else {
				// Save new record
				refinedStudentProfilesRepository.save(refined);
			}
		}
		System.out.println("refinedStudents saved");
	}

	private void updateExistingRecord(RefinedStudentProfiles existing, RefinedStudentProfiles updated) {
		existing.setGender(updated.getGender());
		existing.setAge(updated.getAge());
		existing.setMaritalStatus(updated.getMaritalStatus());
		existing.setChildren(updated.getChildren());
		existing.setCampusId(updated.getCampusId());
		existing.setLevelId(updated.getLevelId());
		existing.setEntryCategoryId(updated.getEntryCategoryId());
		existing.setStudyTimeId(updated.getStudyTimeId());
		existing.setSessionId(updated.getSessionId());
		existing.setNationId(updated.getNationId());
		existing.setInternationalStudent(updated.getInternationalStudent());
		existing.setStudentStatusId(updated.getStudentStatusId());
		existing.setProgramTypeId(updated.getProgramTypeId());
		existing.setAdmissionProgramId(updated.getAdmissionProgramId());
		existing.setFirstChoiceId(updated.getFirstChoiceId());
		existing.setSecondChoiceId(updated.getSecondChoiceId());
		existing.setPreYear(updated.getPreYear());
		existing.setStudyYears(updated.getStudyYears());
		existing.setAcademicYear(updated.getAcademicYear());
		existing.setTuition(updated.getTuition());
		existing.setCgpa(updated.getCgpa());
		existing.setGeneralPaper(updated.getGeneralPaper());
		existing.setUceCredits(updated.getUceCredits());
		existing.setUceDistinctions(updated.getUceDistinctions());
		existing.setOlevelSubjects(updated.getOlevelSubjects());
		existing.setBestSumOutOfTen(updated.getBestSumOutOfTen());
		existing.setAdmissionProgramPriority(updated.getAdmissionProgramPriority());
	}

	public RefinedStudentProfiles transformToRefined(StudentArimsProfiles student) {

		RefinedStudentProfiles refined = new RefinedStudentProfiles();

		// Direct fields
		refined.setAccessNo(student.getAccessNo());
		refined.setTuition(student.getTuition() != null ? student.getTuition() : 0.0);
		refined.setCgpa(student.getCgpa() != null ? student.getCgpa() : 0.0);
		refined.setGeneralPaper(student.getGeneralPaper() != null ? student.getGeneralPaper() : 0L);
		refined.setUceCredits(student.getUceCredits() != null ? student.getUceCredits() : 0L);
		refined.setUceDistinctions(student.getUceDinstinctions() != null ? student.getUceDinstinctions() : 0L);
		refined.setOlevelSubjects(student.getOlevelSubjects() != null ? student.getOlevelSubjects() : 0L);
		refined.setBestSumOutOfTen(student.getBestSumOutOfTen() != null ? student.getBestSumOutOfTen() : 0L);
		refined.setStudyYears(student.getStudyYears() != null ? student.getStudyYears() : 0L);
		refined.setChildren((int) (student.getChildren() != null ? student.getChildren() : 0));

		// Calculated fields
		if (student.getDateOfBirth() != null) {
			int currentYear = LocalDate.now().getYear();
			int birthYear = student.getDateOfBirth().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear();
			refined.setAge(currentYear - birthYear);
		} else {
			refined.setAge(18); // Default age
		}

		// Encoded fields with dynamic mapping and caching
		refined.setGender(getGenderMap()
				.getOrDefault(student.getGender() != null ? student.getGender().toLowerCase() : "unknown", 0));

		refined.setCampusId(getCampusMap()
				.getOrDefault(student.getCampus() != null ? student.getCampus().toLowerCase() : "unknown", 0));

		refined.setLevelId(getLevelMap()
				.getOrDefault(student.getLevel() != null ? student.getLevel().toLowerCase() : "unknown", 0));

		refined.setEntryCategoryId(getEntryCategoryMap().getOrDefault(
				student.getEntryCategory() != null ? student.getEntryCategory().toLowerCase() : "unknown", 0));

		refined.setNationId(getNationMap()
				.getOrDefault(student.getNation() != null ? student.getNation().toLowerCase() : "unknown", 0));

		refined.setStudentStatusId(getStudentStatusMap().getOrDefault(
				student.getStudentStatus() != null ? student.getStudentStatus().toLowerCase() : "unknown", 0));

		refined.setProgramTypeId(getProgramTypeMap().getOrDefault(
				student.getProgramType() != null ? student.getProgramType().toLowerCase() : "unknown", 0));

		refined.setAdmissionProgramId(getAdmissionProgramMap().getOrDefault(
				student.getAdmissionProgram() != null ? student.getAdmissionProgram().toLowerCase() : "unknown", 0));

		// International Student
		refined.setInternationalStudent(
				student.getInternationalStudent() != null && student.getInternationalStudent().equalsIgnoreCase("Y") ? 1
						: 0);

		// Pre-year
		refined.setPreYear(student.getPreYear() != null && student.getPreYear().equalsIgnoreCase("Y") ? 1 : 0);

		// Dynamic Mapping for Marital Status
		String normalizedStatus = normalizeMaritalStatus(
				student.getMaritalStatus() != null ? student.getMaritalStatus() : "unknown");
		refined.setMaritalStatus(getMaritalStatusMap().getOrDefault(normalizedStatus, 0));

		// Academic Year Mapping
		refined.setAcademicYear(getAcademicYearMap().getOrDefault(
				student.getAcademicYear() != null ? student.getAcademicYear().toLowerCase() : "unknown", 0));

		// First Choice Mapping
		refined.setFirstChoiceId(getFirstChoiceMap().getOrDefault(
				student.getFirstChoice() != null ? student.getFirstChoice().toLowerCase() : "unknown", 0));

		// Second Choice Mapping
		refined.setSecondChoiceId(getSecondChoiceMap().getOrDefault(
				student.getSecondChoice() != null ? student.getSecondChoice().toLowerCase() : "unknown", 0));

		// Session Mapping
		refined.setSessionId(getSessionMap()
				.getOrDefault(student.getSession() != null ? student.getSession().toLowerCase() : "unknown", 0));

		// Study Time Mapping
		refined.setStudyTimeId(getStudyTimeMap()
				.getOrDefault(student.getStudyTime() != null ? student.getStudyTime().toLowerCase() : "unknown", 0));

		// Determine Admission Program Priority
		if (student.getAdmissionProgram() != null) {
			String admissionProgramLower = student.getAdmissionProgram().toLowerCase();
			if (student.getFirstChoice() != null
					&& student.getFirstChoice().toLowerCase().equals(admissionProgramLower)) {
				refined.setAdmissionProgramPriority(1); // First Choice
			} else if (student.getSecondChoice() != null
					&& student.getSecondChoice().toLowerCase().equals(admissionProgramLower)) {
				refined.setAdmissionProgramPriority(2); // Second Choice
			} else {
				refined.setAdmissionProgramPriority(0); // Neither
			}
		} else {
			refined.setAdmissionProgramPriority(0); // Default to 0 if no admission program
		}

		// Return the refined object
		return refined;
	}

	/*
	 * Caching and Mapping
	 */
	private Map<String, Integer> cachedCampusMap;
	private Map<String, Integer> cachedGenderMap;
	private Map<String, Integer> cachedLevelMap;
	private Map<String, Integer> cachedEntryCategoryMap;
	private Map<String, Integer> cachedNationMap;
	private Map<String, Integer> cachedStudentStatusMap;
	private Map<String, Integer> cachedProgramTypeMap;
	private Map<String, Integer> cachedAdmissionProgramMap;
	private Map<String, Integer> cachedMaritalStatusMap;
	private Map<String, Integer> cachedAcademicYearMap;
	private Map<String, Integer> cachedFirstChoiceMap;
	private Map<String, Integer> cachedSecondChoiceMap;
	private Map<String, Integer> cachedSessionMap;
	private Map<String, Integer> cachedStudyTimeMap;

	// Getters with Lazy Loading
	public Map<String, Integer> getCampusMap() {
		if (cachedCampusMap == null) {
			cachedCampusMap = generateMap(studentArimsProfilesRepository.findAllUniqueCampuses());
		}
		return cachedCampusMap;
	}

	public Map<String, Integer> getGenderMap() {
		if (cachedGenderMap == null) {
			cachedGenderMap = generateMap(studentArimsProfilesRepository.findAllUniqueGenders());
		}
		return cachedGenderMap;
	}

	public Map<String, Integer> getLevelMap() {
		if (cachedLevelMap == null) {
			cachedLevelMap = generateMap(studentArimsProfilesRepository.findAllUniqueLevels());
		}
		return cachedLevelMap;
	}

	public Map<String, Integer> getEntryCategoryMap() {
		if (cachedEntryCategoryMap == null) {
			cachedEntryCategoryMap = generateMap(studentArimsProfilesRepository.findAllUniqueEntryCategories());
		}
		return cachedEntryCategoryMap;
	}

	public Map<String, Integer> getNationMap() {
		if (cachedNationMap == null) {
			cachedNationMap = generateMap(studentArimsProfilesRepository.findAllUniqueNations());
		}
		return cachedNationMap;
	}

	public Map<String, Integer> getStudentStatusMap() {
		if (cachedStudentStatusMap == null) {
			cachedStudentStatusMap = generateMap(studentArimsProfilesRepository.findAllUniqueStudentStatuses());
		}
		return cachedStudentStatusMap;
	}

	public Map<String, Integer> getProgramTypeMap() {
		if (cachedProgramTypeMap == null) {
			cachedProgramTypeMap = generateMap(studentArimsProfilesRepository.findAllUniqueProgramTypes());
		}
		return cachedProgramTypeMap;
	}

	public Map<String, Integer> getAdmissionProgramMap() {
		if (cachedAdmissionProgramMap == null) {
			cachedAdmissionProgramMap = generateMap(studentArimsProfilesRepository.findAllUniqueAdmissionPrograms());
		}
		return cachedAdmissionProgramMap;
	}

	public Map<String, Integer> getMaritalStatusMap() {
		if (cachedMaritalStatusMap == null) {
			cachedMaritalStatusMap = generateMaritalStatusMap();
		}
		return cachedMaritalStatusMap;
	}

	public Map<String, Integer> getAcademicYearMap() {
		if (cachedAcademicYearMap == null) {
			cachedAcademicYearMap = generateAcademicYearMap();
		}
		return cachedAcademicYearMap;
	}

	public Map<String, Integer> getFirstChoiceMap() {
		if (cachedFirstChoiceMap == null) {
			cachedFirstChoiceMap = generateFirstChoiceMap();
		}
		return cachedFirstChoiceMap;
	}

	public Map<String, Integer> getSecondChoiceMap() {
		if (cachedSecondChoiceMap == null) {
			cachedSecondChoiceMap = generateSecondChoiceMap();
		}
		return cachedSecondChoiceMap;
	}

	public Map<String, Integer> getSessionMap() {
		if (cachedSessionMap == null) {
			cachedSessionMap = generateSessionMap();
		}
		return cachedSessionMap;
	}

	public Map<String, Integer> getStudyTimeMap() {
		if (cachedStudyTimeMap == null) {
			cachedStudyTimeMap = generateStudyTimeMap();
		}
		return cachedStudyTimeMap;
	}

	/*
	 * Helper Methods
	 */

	// Generalized method for dynamic mapping
	private Map<String, Integer> generateMap(List<String> uniqueValues) {
		Map<String, Integer> map = new HashMap<>();
		int index = 1;
		for (String value : uniqueValues) {
			map.put(value.toLowerCase(), index++);
		}
		return map;
	}

	// Specialized method for marital status normalization
	public Map<String, Integer> generateMaritalStatusMap() {
		List<String> uniqueStatuses = studentArimsProfilesRepository.findAllUniqueMaritalStatuses();
		Set<String> normalizedStatuses = uniqueStatuses.stream().map(this::normalizeMaritalStatus)
				.collect(Collectors.toSet());

		Map<String, Integer> maritalStatusMap = new HashMap<>();
		int index = 1;
		for (String status : normalizedStatuses) {
			maritalStatusMap.put(status.toLowerCase(), index++);
		}
		return maritalStatusMap;
	}

	private String normalizeMaritalStatus(String rawStatus) {
		String statusLower = rawStatus.trim().toLowerCase();

		if (statusLower.contains("single") || statusLower.contains("singl") || statusLower.contains("sngle")
				|| statusLower.contains("sigle")) {
			return "Single";
		}

		if (statusLower.contains("married") || statusLower.contains("maried") || statusLower.contains("marred")
				|| statusLower.contains("marr")) {
			return "Married";
		}

		if (statusLower.contains("widow") || statusLower.contains("window")) {
			return "Widowed";
		}

		if (statusLower.contains("separated")) {
			return "Separated";
		}

		return "Other";
	}

	public Map<String, Integer> generateAcademicYearMap() {
		List<String> uniqueAcademicYears = studentArimsProfilesRepository.findAllUniqueAcademicYears();
		return createDynamicMap(uniqueAcademicYears);
	}

	public Map<String, Integer> generateFirstChoiceMap() {
		List<String> uniqueFirstChoices = studentArimsProfilesRepository.findAllUniqueFirstChoices();
		return createDynamicMap(uniqueFirstChoices);
	}

	public Map<String, Integer> generateSecondChoiceMap() {
		List<String> uniqueSecondChoices = studentArimsProfilesRepository.findAllUniqueSecondChoices();
		return createDynamicMap(uniqueSecondChoices);
	}

	public Map<String, Integer> generateSessionMap() {
		List<String> uniqueSessions = studentArimsProfilesRepository.findAllUniqueSessions();
		return createDynamicMap(uniqueSessions);
	}

	public Map<String, Integer> generateStudyTimeMap() {
		List<String> uniqueStudyTimes = studentArimsProfilesRepository.findAllUniqueStudyTimes();
		return createDynamicMap(uniqueStudyTimes);
	}

	private Map<String, Integer> createDynamicMap(List<String> uniqueValues) {
		Map<String, Integer> map = new HashMap<>();
		int index = 1; // Start numbering from 1
		for (String value : uniqueValues) {
			map.put(value != null ? value.toLowerCase() : "unknown", index++);
		}
		return map;
	}

	/*
	 * Prection V2
	 */
	public PredictionInsights predictCgpaV2(PredictionInsightRequest request) {

		PredictionInsights predictionMapper = new PredictionInsights();

		Student student = studentRepository.findByAccessNo(request.getAccessNumber()).orElse(null);

		if (student != null) {

			// Connection and Presence in ARIMS
			StudentArimsProfiles profile = studentArimsProfilesRepository.findByAccessNo(student.getAccessNo())
					.orElse(null);

			if (profile == null) {

				ArimsStudentRegistration arimsOpt = arimsStudentRegistrationRepository
						.findByAccessNumber(student.getAccessNo()).orElse(null);

				// Save Profile
				profile = arimsOpt != null ? generateStudentArimsProfile(arimsOpt) : profile;

			}

			if (profile != null) {

				RefinedStudentProfiles toPredict = null;

				RefinedStudentProfiles refinedProfile = transformToRefined(profile);

				Optional<RefinedStudentProfiles> existingRecord = refinedStudentProfilesRepository
						.findByAccessNo(refinedProfile.getAccessNo());

				if (existingRecord.isPresent()) {
					// Update existing record
					RefinedStudentProfiles existing = existingRecord.get();

					updateExistingRecord(existing, refinedProfile);

					toPredict = refinedStudentProfilesRepository.save(existing);

				} else {
					// Save new record
					toPredict = refinedStudentProfilesRepository.save(refinedProfile);
				}

				if (toPredict != null) {

					JSONObject featurePayload = extractFeaturesFromRefinedStudent(toPredict);

//					System.out.println("featurePayload: " + featurePayload);

					String prediction = predictCGPA(featurePayload);

//					System.out.println("prediction: " + prediction);

					predictionMapper = predictionMapper(prediction);

//					System.out.println("Prediction: " + predictionMapper.getPrediction());

				}

			} else {
				System.out.println("Profile is missing");
			}

		}

		return predictionMapper;

	}

	public JSONObject extractFeaturesFromRefinedStudent(RefinedStudentProfiles student) {
		JSONObject featurePayload = new JSONObject();

		// Numerical Features
		featurePayload.put("general_paper", student.getGeneralPaper() != null ? student.getGeneralPaper() : 0);
		featurePayload.put("tuition", student.getTuition() != null ? student.getTuition() : 0);
		featurePayload.put("uce_credits", student.getUceCredits() != null ? student.getUceCredits() : 0);
		featurePayload.put("uce_distinctions", student.getUceDistinctions() != null ? student.getUceDistinctions() : 0);
		featurePayload.put("campus_id", student.getCampusId() != null ? student.getCampusId() : 0);
		featurePayload.put("study_years", student.getStudyYears() != null ? student.getStudyYears() : 0);
		featurePayload.put("best_sum_out_of_ten",
				student.getBestSumOutOfTen() != null ? student.getBestSumOutOfTen() : 0);
		featurePayload.put("olevel_subjects", student.getOlevelSubjects() != null ? student.getOlevelSubjects() : 0);
		featurePayload.put("age", student.getAge() != null ? student.getAge() : 18); // Default age
		featurePayload.put("admission_program_id",
				student.getAdmissionProgramId() != null ? student.getAdmissionProgramId() : 0);
		featurePayload.put("academic_year_id", student.getAcademicYear() != null ? student.getAcademicYear() : 0);

		// Categorical Features (One-hot encoded)
		featurePayload.put("gender", student.getGender() != null ? student.getGender() : 0);
		featurePayload.put("marital_status", student.getMaritalStatus() != null ? student.getMaritalStatus() : 0);
		featurePayload.put("nation_id", student.getNationId() != null ? student.getNationId() : 0);
		featurePayload.put("student_status_id",
				student.getStudentStatusId() != null ? student.getStudentStatusId() : 0);
		featurePayload.put("entry_category_id",
				student.getEntryCategoryId() != null ? student.getEntryCategoryId() : 0);
		featurePayload.put("program_type_id", student.getProgramTypeId() != null ? student.getProgramTypeId() : 0);
		featurePayload.put("level_id", student.getLevelId() != null ? student.getLevelId() : 0);
		featurePayload.put("session_id", student.getSessionId() != null ? student.getSessionId() : 0);
		featurePayload.put("study_time_id", student.getStudyTimeId() != null ? student.getStudyTimeId() : 0);
		featurePayload.put("first_choice_id", student.getFirstChoiceId() != null ? student.getFirstChoiceId() : 0);
		featurePayload.put("second_choice_id", student.getSecondChoiceId() != null ? student.getSecondChoiceId() : 0);
		featurePayload.put("children", student.getChildren() != null ? student.getChildren() : 0);
		featurePayload.put("pre_year", student.getPreYear() != null ? student.getPreYear() : 0);
		featurePayload.put("international_student",
				student.getInternationalStudent() != null ? student.getInternationalStudent() : 0);
		featurePayload.put("admission_program_priority",
				student.getAdmissionProgramPriority() != null ? student.getAdmissionProgramPriority() : 0);

		// Ensure all expected features exist in the payload
		Set<String> expectedFeatures = generateRefinedExpectedFeatures(); // A method to get all expected features as a
																			// Set
		for (String feature : expectedFeatures) {
			featurePayload.putIfAbsent(feature, 0); // Default to 0 for missing features
		}

		return featurePayload;
	}

	/**
	 * Generate all expected features dynamically (useful for validation).
	 */
	private Set<String> generateRefinedExpectedFeatures() {
		Set<String> features = new HashSet<>();

		// Numerical Features
		features.add("general_paper");
		features.add("tuition");
		features.add("uce_credits");
		features.add("uce_distinctions");
		features.add("campus_id");
		features.add("study_years");
		features.add("best_sum_out_of_ten");
		features.add("olevel_subjects");
		features.add("age");
		features.add("admission_program_id");
		features.add("academic_year_id");

		// Categorical Features
		features.add("gender");
		features.add("marital_status");
		features.add("nation_id");
		features.add("student_status_id");
		features.add("entry_category_id");
		features.add("program_type_id");
		features.add("level_id");
		features.add("session_id");
		features.add("study_time_id");
		features.add("first_choice_id");
		features.add("second_choice_id");
		features.add("children");
		features.add("pre_year");
		features.add("international_student");
		features.add("admission_program_priority");

		return features;
	}

	public ArrayList<PredictionInsights> predictClassCgpaV1(PredictionInsightRequest request) {

		ArrayList<PredictionInsights> predictions = new ArrayList<>();

		List<Student> students = studentRepository.findByCurriculumIdAndYearOfEntry(request.getCurriculumId(),
				request.getEntryYear());

		students = request.getCurrentYear() > 0 ? students.stream()
				.filter(x -> x.getCurrent_year() == request.getCurrentYear()).collect(Collectors.toList()) : students;

		students = request.getCurrentSession() > 0 ? students.stream()
				.filter(x -> x.getCurrentSession() == request.getCurrentSession()).collect(Collectors.toList())
				: students;

		for (Student student : students) {

			PredictionInsightRequest predictionReq = new PredictionInsightRequest(student.getAccessNo());

			try {

				PredictionInsights prediction = predictCgpaV2(predictionReq);

				prediction.setStudentNumber(student.getAccessNo());
				prediction.setCurrentCgpa(student.getCurrentCgpa());

				predictions.add(prediction);

			} catch (Exception e) {
				// TODO: handle exception
			}

		}

		return predictions;

	}

	public ArrayList<RefinedCgpaPrediction> classCgpaPreditions(PredictionInsightRequest request) {

		ArrayList<RefinedCgpaPrediction> predictions = new ArrayList<>();

		ArrayList<PredictionInsights> classPredictions = predictClassCgpaV1(request);

		for (PredictionInsights prediction : classPredictions) {

			if (prediction.getCurrentCgpa() > 0) {

				RefinedCgpaPrediction mappedPrediction = mapPrediction(prediction);

				predictions.add(mappedPrediction);
			}

		}

		return predictions;
	}

	public RefinedCgpaPrediction mapPrediction(PredictionInsights predictionInsights) {
		try {

			return new RefinedCgpaPrediction(predictionInsights.getStudentNumber(), predictionInsights.getCurrentCgpa(),
					predictionInsights.getPrediction().get(0),
					predictionInsights.getInsights().getBenchmarkComparison(),
					predictionInsights.getInsights().getNextSteps(),
					predictionInsights.getInsights().getPredictionConfidence(),
					predictionInsights.getInsights().getSimilarPredictions(), predictionInsights.getStatus(),
					predictionInsights.getInsights().getSensitivity(),
					String.join(", ", predictionInsights.getInsights().getTopFeatures()),
					predictionInsights.getInsights().getTimestamp());

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/*
	 * Version III of data
	 */

	public void understandArims(@Valid PredictionInsightRequest request) {
		// TODO Auto-generated method stub

		Optional<ArimsStudentRegistration> arimsStudent = arimsStudentRegistrationRepository
				.findByAccessNumber(request.getAccessNumber());

		if (arimsStudent.isPresent()) {

			List<Object[]> fullRegistrationData = arimsStudentSemesterRegistrationRepository
					.findFullRegistrationDataByStudentId(arimsStudent.get().getAdminNumber());

			if (fullRegistrationData.isEmpty()) {
				System.out.println(
						"No full registration data found for admin number: " + arimsStudent.get().getAdminNumber());
				return;
			}

			// Process each row (each Object[] represents a full row from the table)
			// Process each row (each Object[] represents a full row from the table)
			for (Object[] row : fullRegistrationData) {
				System.out.println("----- Full Registration Row Details -----");
				// Print the total number of columns in this row
				System.out.println("Total number of columns: " + row.length);

				// Iterate over all columns in the row
				for (int i = 0; i < row.length; i++) {
					String value = (row[i] != null) ? row[i].toString() : "N/A";
					System.out.println("Column " + i + ": " + value);
				}
			}
		} else {
			System.out.println("Student registration not found for access number: " + request.getAccessNumber());
		}

	}

	/*
	 * Version II Data
	 */
	public ArrayList<StudentDataProfilesPayLoad> generateStudentDataProfilesV2() {

		List<Student> students = studentRepository.findEligibleStudentsForGraduation();

		System.out.println("students: " + students.size());

		int count = 0;

		int saved = 0;

		for (Student student : students) {

			ArimsStudentRegistration arimsEntry = arimsStudentRegistrationRepository
					.findByAccessNumber(student.getAccessNo()).orElse(null);

			if (arimsEntry != null && student.getCurrentCgpa() != null && student.getCurrentCgpa() > 0) {

				count++;

				FilteredCompleteStudentProfiles profile = getFilteredProfile(student);

				if (filteredCompleteStudentProfilesRepository.save(profile) != null) {
					saved++;
				}

			}
		}

		System.out.println("Complete Count: " + count);
		System.out.println("Saved Count: " + saved);

		return null;

	}

	private FilteredCompleteStudentProfiles getFilteredProfile(Student student) {

		ArimsStudentRegistration arimsEntry = arimsStudentRegistrationRepository
				.findByAccessNumber(student.getAccessNo()).orElse(null);

		if (arimsEntry != null) {

			FilteredCompleteStudentProfiles profile = filteredCompleteStudentProfilesRepository
					.findByAccessNo(student.getAccessNo()).orElse(new FilteredCompleteStudentProfiles());

			/*
			 * Basic Info
			 */
			profile.setAccessNo(student.getAccessNo());
			profile.setCampusId(student.getCampus() != null ? student.getCampus().getId() : null);
			profile.setIsNational(
					student.getNationality() != null ? student.getNationality().equals(Nationality.NATIONAL) ? 1 : 0
							: 1);
			profile.setYearOfEntry(student.getYearOfEntry() > 0 ? student.getYearOfEntry()
					: generalService.getYearFromDate(Date.from(student.getCreatedAt())));

			profile.setTuition(student.getFeesStructure() != null ? student.getFeesStructure().getNational()
					: arimsEntry.getFees());
			profile.setProgramId(student.getCurriculum() != null ? student.getCurriculum().getProgram().getId() : null);
			profile.setCurriculumId(student.getCurriculum() != null ? student.getCurriculum().getId() : null);
			profile.setLevel(student.getCurriculum() != null
					? student.getCurriculum().getProgram().getProgramCore().getProgramLevel().ordinal()
					: 2);

			/*
			 * Profile Attributes
			 */

			profile.setGender(student.getUser().getPerson().getGender() != null
					? student.getUser().getPerson().getGender().ordinal()
					: null);

			int yearOfEntry = (student.getYearOfEntry() > 0) ? student.getYearOfEntry()
					: generalService.getYearFromDate(Date.from(student.getCreatedAt()));

			int yearOfBirth = (student.getUser() != null && student.getUser().getPerson() != null
					&& student.getUser().getPerson().getDate_of_birth() != null)
							? generalService.getYearFromDate(student.getUser().getPerson().getDate_of_birth())
							: 0;

			// Ensure yearOfEntry is not a future year
			int currentYear = generalService.getYearFromDate(new Date());
			if (yearOfEntry > currentYear) {
				yearOfEntry = currentYear;
			}

			// Ensure yearOfBirth is reasonable (e.g., No student born in the future)
			if (yearOfBirth > currentYear) {
				yearOfBirth = currentYear - 18; // Default to a reasonable birth year
			}

			// Calculate age with added safety checks
			int age = (yearOfBirth > 0 && yearOfEntry > yearOfBirth) ? yearOfEntry - yearOfBirth : 19;

			// Cap the age to a reasonable range (e.g., 15 to 50 years)
			if (age < 15 || age > 50) {
				age = 19; // Assign default age if unrealistic
			}

			// Assign to profile
			profile.setAgeAtEntry(age);

			profile.setMaritalStatus(student.getUser().getPerson().getMarital_status() != null
					? student.getUser().getPerson().getMarital_status().ordinal()
					: 0);
			profile.setChildren(arimsEntry.getChildren());

			/*
			 * Admission Details
			 */

			ProgramCore firstChoice = arimsEntry.getFirstChoice() != null
					? programCoreRepository.findByProgramAbreviation(arimsEntry.getFirstChoice()).stream().findFirst()
							.orElse(null)
					: null;

			ProgramCore secondChoice = arimsEntry.getSecondChoice() != null
					? programCoreRepository.findByProgramAbreviation(arimsEntry.getSecondChoice()).stream().findFirst()
							.orElse(null)
					: null;

			ProgramCore admissionCourse = arimsEntry.getAdminProgram() != null
					? programCoreRepository.findByProgramAbreviation(arimsEntry.getAdminProgram()).stream().findFirst()
							.orElse(null)
					: null;

			profile.setAdmissionRank(extractAdmissionRank(firstChoice, secondChoice, admissionCourse));

			/*
			 * O-Level Results
			 */

			profile.setUceYear(arimsEntry.getUceYear());

			List<ArimsUCEResult> arimsResults = arimsUCEResultRepository.findByRefNumber(arimsEntry.getRefNumber());

			// Extract O-Level features
			Map<String, Object> olevelFeatures = extractOlevelFeatures(arimsResults);

			// Assign extracted values to model
			profile.setOlevelSubjects((Integer) olevelFeatures.get("numOlevelSubjects"));
			profile.setBestSumOutOfSix((Integer) olevelFeatures.get("bestSumOutOfSix"));
			profile.setBestSumOutOfEight((Integer) olevelFeatures.get("bestSumOutOfEight"));
			profile.setBestSumOutOfTen((Integer) olevelFeatures.get("bestSumOutOfTen"));
			profile.setAverageOlevelGrade((Double) olevelFeatures.get("averageOlevelGrade"));
			profile.setHighestOlevelGrade((Integer) olevelFeatures.get("highestOlevelGrade"));
			profile.setLowestOlevelGrade((Integer) olevelFeatures.get("lowestOlevelGrade"));
			profile.setStdDevOlevelGrade((Double) olevelFeatures.get("stdDevOlevelGrade"));
			profile.setUceDinstinctions((Integer) olevelFeatures.get("countDistinctionsOlevel"));
			profile.setUceCredits((Integer) arimsEntry.getUceCredits());
			profile.setCountWeakGradesOlevel((Integer) olevelFeatures.get("countWeakGradesOlevel"));

			/*
			 * A-level Results
			 */

			profile.setUaceYear(arimsEntry.getUaceYear());
			profile.setGeneralPaper((long) arimsEntry.getGeneralPaper());
			profile.setSubjectCombination(arimsEntry.getSubjectCombination());

			List<ArimsUACEResult> alevelResults = arimsUACEResultRepository.findByRefNumber(arimsEntry.getRefNumber());

			Map<String, Object> features = extractAlevelFeatures(alevelResults);

			profile.setAlevelTotalGradeWeight((Integer) features.get("alevelTotalGradeWeight"));
			profile.setAlevelAverageGradeWeight((Double) features.get("alevelAverageGradeWeight"));
			profile.setAlevelNumSubjects((Integer) features.get("alevelNumSubjects"));
			profile.setAlevelHighestGradeWeight((Integer) features.get("alevelHighestGradeWeight"));
			profile.setAlevelLowestGradeWeight((Integer) features.get("alevelLowestGradeWeight"));
			profile.setAlevelStdDevGradeWeight((Double) features.get("alevelStdDevGradeWeight"));
			profile.setAlevelCountDistinctions((Integer) features.get("alevelCountDistinctions"));
			profile.setAlevelCountWeakGrades((Integer) features.get("alevelCountWeakGrades"));
			profile.setAlevelDominantGradeWeight((Integer) features.get("alevelDominantGradeWeight"));

			/*
			 * Secondary School Performance Analysis
			 */
			// Ensure O-Level and A-Level standard deviation values are not null
			Double olevelStdDev = (profile.getStdDevOlevelGrade() != null) ? profile.getStdDevOlevelGrade() : 0.0;
			Double alevelStdDev = (profile.getAlevelStdDevGradeWeight() != null) ? profile.getAlevelStdDevGradeWeight()
					: 0.0;

			// Calculate high school performance variance safely
			double highSchoolPerformanceVariance = (olevelStdDev + alevelStdDev) / 2;

			highSchoolPerformanceVariance = highSchoolPerformanceVariance > 0
					? roundToDecimals(highSchoolPerformanceVariance, 3)
					: highSchoolPerformanceVariance;

			// Avoid division by zero when computing stability index
			double highSchoolPerformanceStabilityIndex = (highSchoolPerformanceVariance > 0)
					? 1 / (1 + highSchoolPerformanceVariance)
					: 1.0; // Defaulting to 1.0 when variance is zero

			highSchoolPerformanceStabilityIndex = highSchoolPerformanceStabilityIndex > 0
					? roundToDecimals(highSchoolPerformanceStabilityIndex, 3)
					: highSchoolPerformanceStabilityIndex;

			// Assign values to the profile
			profile.setHighSchoolPerformanceVariance(highSchoolPerformanceVariance);
			profile.setHighSchoolPerformanceStabilityIndex(highSchoolPerformanceStabilityIndex);

			/*
			 * Campus Results
			 */
			profile.setFinalCgpa(student.getCurrentCgpa());

			return profile;
		}

		return null;
	}

	public ArrayList<StudentDataProfilesPayLoad> generateStudentDataProfilesV2Tem() {

		List<Student> students = studentRepository.findAll();

		System.out.println("students: " + students.size());

		// A77051
		int arims = 0;
		int applicants = 0;

		for (Student student : students) {

			// Connection and Presence in ARIMS
			Optional<ArimsStudentRegistration> arimsOpt = student.getAccessNo() != null
					? arimsStudentRegistrationRepository.findByAccessNumber(student.getAccessNo())
					: Optional.empty();

			// First Application system
			Applicant applicant = student.getApplicant() != null ? student.getApplicant() : null;

			if (arimsOpt.isPresent()) {

				StudentApplicantProfile profile = saveApplicantProfile(student, "arims");

				String referenceNumber = arimsOpt.get().getRefNumber();

				ArimsSubjectRequest arimsDetails = arimsMappingService.getApplicantArimsGrades(referenceNumber);

				profile.setFirstChoice(arimsDetails.getFirstChoice());
				profile.setSecondChoice(arimsDetails.getSecondChoice());
				profile.setAdmissionProgram(arimsDetails.getAdmissionCourse());
				profile.setApplicationDate(arimsDetails.getApplicationDate());

				Optional<ArimsStudentRegistration> arimsStudent = arimsStudentRegistrationRepository
						.findByRefNumber(referenceNumber);

				if (arimsStudent.isPresent()) {

					profile.setAcademicYear(arimsStudent.get().getAcademicYear());
					profile.setCalendarYear(
							arimsStudent.get().getCalendarYear() != null ? (long) arimsStudent.get().getCalendarYear()
									: null);

					profile.setUceCenter(arimsStudent.get().getUceCenter());
					profile.setUceCredits((long) arimsStudent.get().getUceCredits());
					profile.setUceDinstinctions((long) arimsStudent.get().getUceDinstinctions());
					profile.setUceIndexNumber(arimsStudent.get().getUceIndexNumber());
					profile.setUceYear(arimsStudent.get().getUceYear());

					profile.setUaceCenter(arimsStudent.get().getUaceCenter());
					profile.setUaceIndexNumber(arimsStudent.get().getUaceIndexNumber());
					profile.setUaceYear(arimsStudent.get().getUaceYear());
					profile.setGeneralPaper((long) arimsStudent.get().getGeneralPaper());
					profile.setSubjectCombination(arimsStudent.get().getSubjectCombination());

				}

				if (studentApplicantProfileRepository.save(profile) != null && arimsStudent.isPresent()) {

					// Save Grades
					saveOLevelResults(arimsStudent.get(), profile);

				}

				if (profile != null) {
					arims += 1;
				}

			}

			else if (applicant != null) {

				StudentApplicantProfile profile = saveApplicantProfile(student, "alpha");

				if (profile != null) {
					applicants += 1;
				}

			} else {
				System.out.println("Student Applicant not found");
			}

		}

		System.out.println("Arims Students: " + arims);
		System.out.println("1st Application: " + applicants);

		return null;
	}

	public static Map<String, Object> extractAlevelFeatures(List<ArimsUACEResult> results) {
		if (results == null || results.isEmpty()) {
			return Collections.emptyMap();
		}

		// Initialize variables
		int totalGradeWeight = 0;
		int highestGradeWeight = Integer.MIN_VALUE;
		int lowestGradeWeight = Integer.MAX_VALUE;
		int countDistinctions = 0;
		int countWeakGrades = 0;
		Map<Integer, Integer> gradeFrequency = new HashMap<>();

		List<Integer> gradeWeights = new ArrayList<>();

		for (ArimsUACEResult result : results) {
			int weight = result.getGradeWeight();

			// Aggregate values
			totalGradeWeight += weight;
			gradeWeights.add(weight);

			// Track highest and lowest grade weight
			highestGradeWeight = Math.max(highestGradeWeight, weight);
			lowestGradeWeight = Math.min(lowestGradeWeight, weight);

			// Count distinctions (GradeWeight ≥ 5)
			if (weight >= 5) {
				countDistinctions++;
			}

			// Count weak grades (GradeWeight ≤ 3)
			if (weight <= 3) {
				countWeakGrades++;
			}

			// Count frequency of grade weight occurrences
			gradeFrequency.put(weight, gradeFrequency.getOrDefault(weight, 0) + 1);
		}

		int numSubjects = gradeWeights.size();
		double averageGradeWeight = numSubjects > 0 ? (double) totalGradeWeight / numSubjects : 0.0;

		averageGradeWeight = averageGradeWeight > 0 ? roundToDecimals(averageGradeWeight, 2) : averageGradeWeight;

		// Calculate standard deviation
		double stdDevGradeWeight = calculateStandardDeviation(gradeWeights, averageGradeWeight);

		stdDevGradeWeight = stdDevGradeWeight > 0 ? roundToDecimals(stdDevGradeWeight, 3) : stdDevGradeWeight;

		// Find the dominant grade weight (most frequent)
		int dominantGradeWeight = gradeFrequency.entrySet().stream().max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey).orElse(0); // Default to 0 if no valid data

		// Store results in a map
		Map<String, Object> features = new HashMap<>();
		features.put("alevelTotalGradeWeight", totalGradeWeight);
		features.put("alevelAverageGradeWeight", averageGradeWeight);
		features.put("alevelNumSubjects", numSubjects);
		features.put("alevelHighestGradeWeight", highestGradeWeight);
		features.put("alevelLowestGradeWeight", lowestGradeWeight);
		features.put("alevelStdDevGradeWeight", stdDevGradeWeight);
		features.put("alevelCountDistinctions", countDistinctions);
		features.put("alevelCountWeakGrades", countWeakGrades);
		features.put("alevelDominantGradeWeight", dominantGradeWeight);

		return features;
	}

	// Helper method to compute standard deviation
	private static double calculateStandardDeviation(List<Integer> values, double mean) {
		if (values.isEmpty())
			return 0.0;

		double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / values.size();

		return Math.sqrt(variance);
	}

	/*
	 * O level Mappings
	 */
	public static Map<String, Object> extractOlevelFeatures(List<ArimsUCEResult> results) {
		if (results == null || results.isEmpty()) {
			return Collections.emptyMap();
		}

		// Initialize variables
		int numSubjects = results.size();
		int totalGradeSum = 0;
		int highestGrade = Integer.MAX_VALUE;
		int lowestGrade = Integer.MIN_VALUE;
		int countDistinctions = 0;
		int countWeakGrades = 0;

		List<Integer> grades = new ArrayList<>();

		for (ArimsUCEResult result : results) {
			int grade = result.getGrade();

			// Aggregate values
			totalGradeSum += grade;
			grades.add(grade);

			// Track highest and lowest grades
			highestGrade = Math.min(highestGrade, grade);
			lowestGrade = Math.max(lowestGrade, grade);

			// Count distinctions (Grade ≤ 2)
			if (grade <= 2) {
				countDistinctions++;
			}

			// Count weak grades (Grade ≥ 6)
			if (grade >= 6) {
				countWeakGrades++;
			}
		}

		// Compute average grade
		double averageGrade = numSubjects > 0 ? (double) totalGradeSum / numSubjects : 0.0;

		averageGrade = averageGrade > 0 ? roundToDecimals(averageGrade, 2) : averageGrade;

		// Compute standard deviation
		double stdDevGrade = calculateOLevelStandardDeviation(grades, averageGrade);

		stdDevGrade = stdDevGrade > 0 ? roundToDecimals(stdDevGrade, 3) : stdDevGrade;

		// Compute best sum calculations
		int bestSumOutOfSix = calculateOLevelBestSum(results, 6);
		int bestSumOutOfEight = calculateOLevelBestSum(results, 8);
		int bestSumOutOfTen = calculateOLevelBestSum(results, 10);

		// Store results in a map
		Map<String, Object> features = new HashMap<>();
		features.put("numOlevelSubjects", numSubjects);
		features.put("bestSumOutOfSix", bestSumOutOfSix);
		features.put("bestSumOutOfEight", bestSumOutOfEight);
		features.put("bestSumOutOfTen", bestSumOutOfTen);
		features.put("averageOlevelGrade", averageGrade);
		features.put("highestOlevelGrade", highestGrade);
		features.put("lowestOlevelGrade", lowestGrade);
		features.put("stdDevOlevelGrade", stdDevGrade);
		features.put("countDistinctionsOlevel", countDistinctions);
		features.put("countWeakGradesOlevel", countWeakGrades);

		return features;
	}

	// Helper method to compute standard deviation
	private static double calculateOLevelStandardDeviation(List<Integer> values, double mean) {
		if (values.isEmpty())
			return 0.0;

		double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / values.size();

		return Math.sqrt(variance);
	}

	// Calculate best sum for top N grades
	private static int calculateOLevelBestSum(List<ArimsUCEResult> results, int count) {
		List<ArimsUCEResult> sortedResults = results.stream().sorted(Comparator.comparingInt(ArimsUCEResult::getGrade))
				.collect(Collectors.toList());

		int totalSum = 0;
		for (int i = 0; i < count && i < sortedResults.size(); i++) {
			totalSum += sortedResults.get(i).getGrade();
		}
		return totalSum;
	}

	public int extractAdmissionRank(ProgramCore firstChoice, ProgramCore secondChoice, ProgramCore admissionCourse) {
		if (admissionCourse == null) {
			return 4; // No admission data available
		}

		// Check if first choice is valid & matches admission
		if (firstChoice != null && firstChoice.getId() != null && firstChoice.getId().equals(admissionCourse.getId())) {
			return 1; // Admitted to First Choice
		}

		// Check if second choice is valid & matches admission
		if (secondChoice != null && secondChoice.getId() != null
				&& secondChoice.getId().equals(admissionCourse.getId())) {
			return 2; // Admitted to Second Choice
		}

		return 3; // Admitted to an unchosen program
	}

	public static double roundToDecimals(Double value, int places) {
		if (value == null)
			return 0.0; // Handle null values safely
		if (places < 0)
			throw new IllegalArgumentException("Decimal places must be non-negative");

		return new BigDecimal(value).setScale(places, RoundingMode.HALF_UP) // Uses standard rounding method
				.doubleValue();
	}

	/*
	 * Data Description
	 */
	public void generateDataSummary() {

		List<FilteredCompleteStudentProfiles> students = filteredCompleteStudentProfilesRepository.findAll();

		System.out.println("📊 **Expanded Dataset Summary for CGPA Prediction Research**");
		System.out.println("----------------------------------------------------");

		// Total records
		System.out.println("✅ Total student records: " + students.size());

		// Gender distribution
		Map<String, Long> genderCounts = students.stream()
				.collect(Collectors.groupingBy(s -> (s.getGender() == null) ? "Unknown" // Handle null values
						: (s.getGender() == 0) ? "Female" : (s.getGender() == 1) ? "Male" : "Other",
						Collectors.counting()));

		System.out.println("✅ Gender Distribution:");
		genderCounts.forEach((gender, count) -> System.out.println("   - " + gender + ": " + count));

		// Nationality distribution
		long nationalCount = students.stream().filter(s -> s.getIsNational() == 1).count();
		long internationalCount = students.stream().filter(s -> s.getIsNational() == 0).count();
		System.out.println("✅ Nationality Distribution:");
		System.out.println("   - National Students: " + nationalCount);
		System.out.println("   - International Students: " + internationalCount);

		// Age at entry statistics
		DoubleSummaryStatistics ageStats = students.stream()
				.filter(s -> s.getAgeAtEntry() != null && s.getAgeAtEntry() > 0)
				.mapToDouble(s -> s.getAgeAtEntry().doubleValue()) // Convert Integer to Double
				.summaryStatistics();

		System.out.println("✅ Age at Entry Statistics:");
		System.out.println("   - Minimum Age: " + ageStats.getMin());
		System.out.println("   - Maximum Age: " + ageStats.getMax());
		System.out.println("   - Average Age: " + String.format("%.2f", ageStats.getAverage()));

		// Tuition statistics
		DoubleSummaryStatistics tuitionStats = students.stream().filter(s -> s.getTuition() > 0)
				.mapToDouble(FilteredCompleteStudentProfiles::getTuition).summaryStatistics();

		System.out.println("✅ Tuition Fee Statistics:");
		System.out.println("   - Minimum Tuition: " + tuitionStats.getMin());
		System.out.println("   - Maximum Tuition: " + tuitionStats.getMax());
		System.out.println("   - Average Tuition: " + String.format("%.2f", tuitionStats.getAverage()));

		// Marital status distribution
		Map<Integer, Long> maritalStatusCounts = students.stream().collect(
				Collectors.groupingBy(FilteredCompleteStudentProfiles::getMaritalStatus, Collectors.counting()));

		System.out.println("✅ Marital Status Distribution:");
		maritalStatusCounts.forEach((status, count) -> {
			String statusLabel = switch (status) {
			case 0 -> "Single";
			case 1 -> "Married";
			case 2 -> "Divorced";
			default -> "Unknown";
			};
			System.out.println("   - " + statusLabel + ": " + count);
		});

		// Admission ranking
		Map<Integer, Long> admissionCounts = students.stream().collect(
				Collectors.groupingBy(FilteredCompleteStudentProfiles::getAdmissionRank, Collectors.counting()));

		System.out.println("✅ Admission Rank Distribution:");
		admissionCounts.forEach((rank, count) -> {
			String rankLabel = switch (rank) {
			case 1 -> "Admitted to First Choice";
			case 2 -> "Admitted to Second Choice";
			case 3 -> "Admitted to Unchosen Program";
			default -> "Unknown Admission Status";
			};
			System.out.println("   - " + rankLabel + ": " + count);
		});

		// CGPA classification
		System.out.println("✅ CGPA Classification (Based on Uganda's Degree System):");
		long firstClass = students.stream().filter(s -> s.getFinalCgpa() != null && s.getFinalCgpa() >= 4.40).count();
		long secondUpper = students.stream()
				.filter(s -> s.getFinalCgpa() != null && s.getFinalCgpa() >= 3.60 && s.getFinalCgpa() < 4.40).count();
		long secondLower = students.stream()
				.filter(s -> s.getFinalCgpa() != null && s.getFinalCgpa() >= 2.80 && s.getFinalCgpa() < 3.60).count();
		long pass = students.stream()
				.filter(s -> s.getFinalCgpa() != null && s.getFinalCgpa() >= 2.00 && s.getFinalCgpa() < 2.80).count();
		long fail = students.stream().filter(s -> s.getFinalCgpa() != null && s.getFinalCgpa() < 2.00).count();

		System.out.println("   - First Class (CGPA 4.40+): " + firstClass);
		System.out.println("   - Second Class Upper (CGPA 3.60 - 4.39): " + secondUpper);
		System.out.println("   - Second Class Lower (CGPA 2.80 - 3.59): " + secondLower);
		System.out.println("   - Pass (CGPA 2.00 - 2.79): " + pass);
		System.out.println("   - Fail (CGPA < 2.00): " + fail);

		// Program distribution with program names
		Map<Long, Long> programCounts = students.stream().filter(s -> s.getProgramId() != null)
				.collect(Collectors.groupingBy(FilteredCompleteStudentProfiles::getProgramId, Collectors.counting()));

		System.out.println("✅ Number of Unique Programs: " + programCounts.size());
		System.out.println("✅ Students Per Program:");

		programCounts.forEach((programId, count) -> {
			String programName = programRepository.findById(programId).map(p -> p.getProgramCore().getProgramName())
					.orElse("Unknown Program");

			System.out.println("   - " + programName + ": " + count + " students");
		});

		// Level distribution with readable names
		Map<Integer, Long> levelCounts = students.stream()
				.collect(Collectors.groupingBy(FilteredCompleteStudentProfiles::getLevel, Collectors.counting()));

		System.out.println("✅ Students Per Program Level:");

		levelCounts.forEach((levelOrdinal, count) -> {
			String programLevel = getProgramLevelName(levelOrdinal);
			System.out.println("   - " + programLevel + ": " + count + " students");
		});

		// CGPA Distribution by Program with program names
		Map<Long, DoubleSummaryStatistics> cgpaByProgram = students.stream()
				.filter(s -> s.getProgramId() != null && s.getFinalCgpa() != null)
				.collect(Collectors.groupingBy(FilteredCompleteStudentProfiles::getProgramId,
						Collectors.summarizingDouble(FilteredCompleteStudentProfiles::getFinalCgpa)));

		System.out.println("✅ CGPA Distribution by Program:");

		cgpaByProgram.forEach((programId, stats) -> {
			String programName = programRepository.findById(programId).map(p -> p.getProgramCore().getProgramName())
					.orElse("Unknown Program");

			System.out.println("   - " + programName + ": Avg CGPA = " + String.format("%.2f", stats.getAverage()));
		});

		System.out.println("----------------------------------------------------");
		System.out.println("📌 Expanded data summary completed successfully!");
	}

	/**
	 * Helper method to get program level name from ordinal value.
	 */
	private String getProgramLevelName(Integer ordinal) {
		if (ordinal == null || ordinal < 0 || ordinal >= ProgramLevel.values().length) {
			return "Unknown";
		}
		return ProgramLevel.values()[ordinal].name().replace("_", " "); // Format name for better readability
	}

	public void generateProgramNames() {

		for (Program program : programRepository.findAll()) {

			if (program.getId() != null) {

				ProgramMappings mapping = programMappingsRepository.findByProgramId(program.getId())
						.orElse(new ProgramMappings());

				mapping.setProgramId(program.getId());
				mapping.setProgramName(program.getProgramCore().getProgramName());

				programMappingsRepository.save(mapping);
			}

		}

	}

	/*
	 * Filtered Predictions
	 */
	public Double predictStudentCGPA(PredictionInsightRequest request) {

		Student student = studentRepository.findByAccessNo(request.getAccessNumber()).orElse(null);

		if (student != null) {

			JSONObject studentJson = generateStudentJson(student);
			String predictionResponse = sendPredictionRequest(studentJson);
			System.out.println(predictionResponse);

		}
		return null;
	}

	public String sendPredictionRequest(JSONObject json) {
		try {
			System.out.println("Sending JSON: " + json.toString()); // Debugging JSON payload

			URL url = new URL("http://127.0.0.1:5000/predict");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);

			String jsonInputString = json.toString();
			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}

			int responseCode = conn.getResponseCode();
			System.out.println("Response Code: " + responseCode); // Debugging response

			if (responseCode == 200) {
				try (java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream(),
						StandardCharsets.UTF_8.name())) {
					return scanner.useDelimiter("\\A").next();
				}
			} else {
				try (java.util.Scanner scanner = new java.util.Scanner(conn.getErrorStream(),
						StandardCharsets.UTF_8.name())) {
					return "Error " + responseCode + ": " + scanner.useDelimiter("\\A").next();
				}
			}
		} catch (Exception e) {
			return "Exception: " + e.getMessage();
		}
	}

	/*
	 * Filtered Model Prediction: JSON Object Generation
	 */

	public JSONObject generateStudentJson(Student student) {
		ArimsStudentRegistration arimsEntry = arimsStudentRegistrationRepository
				.findByAccessNumber(student.getAccessNo()).orElse(null);

		if (arimsEntry != null && student.getCurrentCgpa() != null && student.getCurrentCgpa() > 0) {

			FilteredCompleteStudentProfiles profile = getFilteredProfile(student);

			// Strict order: Ensure feature order matches the Flask model
			String[] featureOrder = { "admission_rank", "age_at_entry", "alevel_average_grade_weight",
					"alevel_count_distinctions", "alevel_count_weak_grades", "alevel_dominant_grade_weight",
					"alevel_highest_grade_weight", "alevel_lowest_grade_weight", "alevel_num_subjects",
					"alevel_std_dev_grade_weight", "alevel_total_grade_weight", "average_olevel_grade",
					"best_sum_out_of_eight", "best_sum_out_of_six", "best_sum_out_of_ten", "campus_id",
					"count_weak_grades_olevel", "curriculum_id", "gender", "general_paper",
					"high_school_performance_stability_index", "high_school_performance_variance",
					"highest_olevel_grade", "is_national", "level", "lowest_olevel_grade", "marital_status",
					"olevel_subjects", "program_id", "std_dev_olevel_grade", "tuition", "uace_year", "uce_credits",
					"uce_distinctions", "uce_year", "year_of_entry", "program_category_encoded" };

			// Store data in a LinkedHashMap to ensure the order
			Map<String, Object> orderedJsonMap = new LinkedHashMap<>();

			// Assign values in the exact order expected by Flask
			for (String feature : featureOrder) {
				switch (feature) {
				case "admission_rank":
					orderedJsonMap.put(feature, getOrDefault(profile.getAdmissionRank(), 0));
					break;
				case "age_at_entry":
					orderedJsonMap.put(feature, getOrDefault(profile.getAgeAtEntry(), 18));
					break;
				case "alevel_average_grade_weight":
					orderedJsonMap.put(feature, getOrDefault(profile.getAlevelAverageGradeWeight(), 0.0));
					break;
				case "alevel_count_distinctions":
					orderedJsonMap.put(feature, getOrDefault(profile.getAlevelCountDistinctions(), 0));
					break;
				case "alevel_count_weak_grades":
					orderedJsonMap.put(feature, getOrDefault(profile.getAlevelCountWeakGrades(), 0));
					break;
				case "alevel_dominant_grade_weight":
					orderedJsonMap.put(feature, getOrDefault(profile.getAlevelDominantGradeWeight(), 0));
					break;
				case "alevel_highest_grade_weight":
					orderedJsonMap.put(feature, getOrDefault(profile.getAlevelHighestGradeWeight(), 0));
					break;
				case "alevel_lowest_grade_weight":
					orderedJsonMap.put(feature, getOrDefault(profile.getAlevelLowestGradeWeight(), 0));
					break;
				case "alevel_num_subjects":
					orderedJsonMap.put(feature, getOrDefault(profile.getAlevelNumSubjects(), 0));
					break;
				case "alevel_std_dev_grade_weight":
					orderedJsonMap.put(feature, getOrDefault(profile.getAlevelStdDevGradeWeight(), 0.0));
					break;
				case "alevel_total_grade_weight":
					orderedJsonMap.put(feature, getOrDefault(profile.getAlevelTotalGradeWeight(), 0));
					break;
				case "average_olevel_grade":
					orderedJsonMap.put(feature, getOrDefault(profile.getAverageOlevelGrade(), 0.0));
					break;
				case "best_sum_out_of_eight":
					orderedJsonMap.put(feature, getOrDefault(profile.getBestSumOutOfEight(), 0));
					break;
				case "best_sum_out_of_six":
					orderedJsonMap.put(feature, getOrDefault(profile.getBestSumOutOfSix(), 0));
					break;
				case "best_sum_out_of_ten":
					orderedJsonMap.put(feature, getOrDefault(profile.getBestSumOutOfTen(), 0));
					break;
				case "campus_id":
					orderedJsonMap.put(feature, getOrDefault(profile.getCampusId(), 0));
					break;
				case "count_weak_grades_olevel":
					orderedJsonMap.put(feature, getOrDefault(profile.getCountWeakGradesOlevel(), 0));
					break;
				case "curriculum_id":
					orderedJsonMap.put(feature, getOrDefault(profile.getCurriculumId(), 0));
					break;
				case "gender":
					orderedJsonMap.put(feature, getOrDefault(profile.getGender(), 0));
					break;
				case "general_paper":
					orderedJsonMap.put(feature, getOrDefault(profile.getGeneralPaper(), 0));
					break;
				case "high_school_performance_stability_index":
					orderedJsonMap.put(feature, getOrDefault(profile.getHighSchoolPerformanceStabilityIndex(), 1.0));
					break;
				case "high_school_performance_variance":
					orderedJsonMap.put(feature, getOrDefault(profile.getHighSchoolPerformanceVariance(), 0.0));
					break;
				case "highest_olevel_grade":
					orderedJsonMap.put(feature, getOrDefault(profile.getHighestOlevelGrade(), 0));
					break;
				case "is_national":
					orderedJsonMap.put(feature, getOrDefault(profile.getIsNational(), 1));
					break;
				case "level":
					orderedJsonMap.put(feature, getOrDefault(profile.getLevel(), 0));
					break;
				case "lowest_olevel_grade":
					orderedJsonMap.put(feature, getOrDefault(profile.getLowestOlevelGrade(), 0));
					break;
				case "marital_status":
					orderedJsonMap.put(feature, getOrDefault(profile.getMaritalStatus(), 0));
					break;
				case "olevel_subjects":
					orderedJsonMap.put(feature, getOrDefault(profile.getOlevelSubjects(), 0));
					break;
				case "program_id":
					orderedJsonMap.put(feature, getOrDefault(profile.getProgramId(), 0));
					break;
				case "std_dev_olevel_grade":
					orderedJsonMap.put(feature, getOrDefault(profile.getStdDevOlevelGrade(), 0.0));
					break;
				case "tuition":
					orderedJsonMap.put(feature, getOrDefault(profile.getTuition(), 0.0));
					break;
				case "uace_year":
					orderedJsonMap.put(feature, getOrDefault(profile.getUaceYear(), "0000"));
					break;
				case "uce_credits":
					orderedJsonMap.put(feature, getOrDefault(profile.getUceCredits(), 0));
					break;
				case "uce_distinctions":
					orderedJsonMap.put(feature, getOrDefault(profile.getUceDinstinctions(), 0));
					break;
				case "uce_year":
					orderedJsonMap.put(feature, getOrDefault(profile.getUceYear(), "0000"));
					break;
				case "year_of_entry":
					orderedJsonMap.put(feature, getOrDefault(profile.getYearOfEntry(), 2000));
					break;
				case "program_category_encoded":
					orderedJsonMap.put(feature,
							classifyProgram(student.getCurriculum().getProgram().getProgramCore().getProgramName()));
					break;
				}
			}

			// Convert ordered map to JSONObject
			return new JSONObject(orderedJsonMap);
		}
		return null;
	}

// Utility method to handle null values
	private <T> T getOrDefault(T value, T defaultValue) {
		return value != null ? value : defaultValue;
	}

	public static int classifyProgram(String name) {
		// Define keyword categories
		String[] scienceKeywords = { "Science", "Engineering", "Technology", "Mathematics", "Physics", "Biology",
				"Chemistry" };
		String[] artsKeywords = { "Arts", "Literature", "Philosophy", "Education", "Theology", "Social" };
		String[] businessKeywords = { "Business", "Management", "Accounting", "Finance", "Economics" };

		// Convert name to lowercase for case-insensitive comparison
		String nameLower = name.toLowerCase();

		// Check for science category
		for (String keyword : scienceKeywords) {
			if (nameLower.contains(keyword.toLowerCase())) {
				return 1; // Science
			}
		}

		// Check for arts category
		for (String keyword : artsKeywords) {
			if (nameLower.contains(keyword.toLowerCase())) {
				return 2; // Arts
			}
		}

		// Check for business category
		for (String keyword : businessKeywords) {
			if (nameLower.contains(keyword.toLowerCase())) {
				return 3; // Business
			}
		}

		// Default category for other programs
		return 4; // Other
	}

}
