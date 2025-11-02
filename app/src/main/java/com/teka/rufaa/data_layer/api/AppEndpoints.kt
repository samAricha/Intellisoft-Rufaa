package com.teka.rufaa.data_layer.api;

object AppEndpoints {

    //defualt url
    const val DEFAULT_BASE_URL = "https://patientvisitapis.intellisoftkenya.com/api/"

    // Auth
    const val SIGN_IN = "user/signin"


    // Patient endpoints
    const val PATIENTS_VIEW = "patients/view"
    const val PATIENT_DETAILS = "patients/show/"
    const val PATIENTS_REGISTER = "patients/register"
    const val VITALS_ADD = "vital/add"
    const val VISITS_ADD = "visits/add"

}