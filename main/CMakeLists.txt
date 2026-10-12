idf_component_register(
    SRCS "main.c"
         "passport_app_model.c"
         "passport_ble_link.c"
         "passport_location_model.c"
         "passport_location_protocol.c"
         "passport_store.c"
         "passport_trip_model.c"
         "passport_trip_store.c"
         "passport_display_policy.c"
    INCLUDE_DIRS "."
    REQUIRES bsp bt esp_hw_support esp_timer nvs_flash qrcode
)

target_sources(${COMPONENT_LIB} PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/../assets/fonts/passport_gb2312_14.c"
    "${CMAKE_CURRENT_LIST_DIR}/../assets/fonts/passport_gb2312_20.c"
)
