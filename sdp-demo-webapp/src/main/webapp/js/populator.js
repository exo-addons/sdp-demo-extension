(function($) {

    $( function () {
        $("#selectScenario").change(function () {
            $("#selectScenario option:not(:selected)").each(function () {
                $("#description-" + $(this).val()).hide();
            });

            var selectedScenarioName = $('#selectScenario').find(":selected").val();
            if (selectedScenarioName == "") {
                $(".btn-start").addClass("disabled");
            } else {
                $(".btn-start").removeClass("disabled");
            }
            $("#description-" + selectedScenarioName).show();
        });

        $(".btn-start").on("click", function () {
            $(".btn-start").addClass("disabled");
            var selectedScenarioName = $('#selectScenario').find(":selected").val();

            var $populatorDiv = $("#populator_div");
            var jzStartPopulate = $populatorDiv.jzURL("Populator.populate");

            if (selectedScenarioName != "") {
                $.ajax({
                    url: jzStartPopulate,
                    dataType: "json",
                    data: {
                        "scenarioName": selectedScenarioName
                    },
                    context: this,
                    success: function (data) {
                        console.log(data.status);
                        $(".btn-start").removeClass("disabled");
                    },
                    error: function () {
                        console.log("error in server call");
                    }
                });
            }

        });
    });

})(jQuery);