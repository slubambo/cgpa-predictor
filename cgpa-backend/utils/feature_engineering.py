def transform_input(input_data):
    data = input_data.dict()
    feature_order = [
        'age_at_entry', 'average_olevel_grade', 'uce_credits',
        'best_sum_out_of_six', 'best_sum_out_of_eight', 'best_sum_out_of_ten',
        'count_weak_grades_olevel', 'highest_olevel_grade', 'std_dev_olevel_grade',
        'alevel_average_grade_weight', 'alevel_total_grade_weight', 'alevel_std_dev_grade_weight',
        'alevel_dominant_grade_weight', 'alevel_count_weak_grades', 'gender', 'level',
        'campus_id_code', 'program_id_code', 'curriculum_id_code', 'year_of_entry_code',
        'uce_year_code', 'uace_year_code', 'high_school_performance_variance',
        'high_school_performance_stability_index', 'lowest_olevel_grade', 'general_paper'
    ]
    return [data.get(col, 0) for col in feature_order]

def classify_band(cgpa: float) -> str:
    if cgpa >= 3.6:
        return "High"
    elif cgpa >= 2.8:
        return "Moderate"
    else:
        return "Low"
