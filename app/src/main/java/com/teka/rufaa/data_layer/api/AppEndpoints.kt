package com.teka.rufaa.data_layer.api;

object AppEndpoints {

    //defualt url
    const val DEFAULT_BASE_URL = "https://patientvisitapis.intellisoftkenya.com/api/"

    // Auth
    const val SIGN_IN = "android_check_user"

    // Gatelogs
    const val GATE_LOGS = "packhouseGatelogsData"

    //Suppliers
    const val GET_SUPPLIERS = "android_packhouse_get_suppliers"

    //Intakes
    const val GET_INTAKES_ANDROID= "android_packhouse_get_intake"
    const val GATELOG_SUPPLIERS = "packhouseGatelogSuppliersData"
    const val POST_INTAKES= "packhouseIntakeData"
    const val GET_INTAKES= "packhouseIntakeData"
    const val AUTO_CREATE_AVO_INTAKE_PALLETS= "autocreateAvoIntakePallets"

    //Receiving
    const val RECEIVING = "packhouseReceivingData"

    //Qc
    const val QC = "oilQcData"
    const val QC_OIL_QUALITY_ANALYSIS = "oilQualityAnalysisData"

    //Oil Binning
    const val OIL_BINNING = "oilBinningData"



    // Avo Rejects

    

}