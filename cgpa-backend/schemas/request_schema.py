from pydantic import BaseModel

class StudentInput(BaseModel):
    age_at_entry: int
    gender: int
    level: int

    uce_year_code: int
    uce_credits: int
    average_olevel_grade: float
    best_sum_out_of_six: int
    best_sum_out_of_eight: int
    best_sum_out_of_ten: int
    count_weak_grades_olevel: int
    highest_olevel_grade: int
    lowest_olevel_grade: int
    std_dev_olevel_grade: float

    uace_year_code: int
    general_paper: int
    alevel_average_grade_weight: float
    alevel_total_grade_weight: float
    alevel_std_dev_grade_weight: float
    alevel_dominant_grade_weight: float
    alevel_count_weak_grades: int

    year_of_entry_code: int
    campus_id_code: int
    program_id_code: int
    curriculum_id_code: int

    high_school_performance_variance: float
    high_school_performance_stability_index: float
