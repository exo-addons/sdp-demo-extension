(function ($) {

    $(function () {
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
            $(".progressionContainer").show();

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
                        clearInterval(refreshInterval);
                        refresh();
                        if (data.downloadUrl && data.downloadUrl != "") {
                            $(".scenarioResultContent").html("Scenario datas correctly populated. Content to run the scenario script are available to <a href='" + data.downloadUrl + "'>download here</a>.");
                            $(".scenarioResult").show();
                        }
                        $(".btn-start").removeClass("disabled");
                    },
                    error: function (data) {
                        $("#error").show();
                        $(".btn-start").removeClass("disabled");

                    }
                });
            }

        });

        function refresh() {
            var $populatorDiv = $("#populator_div");

            var jzRefreshElements = $populatorDiv.jzURL("Populator.elements");
            jQuery.ajax({
                url: jzRefreshElements,
                dataType: "json",
                context: this,
                success: function (data) {
                    //      console.log("Refresh Elements");
                    updateElementsContainer(data);
                },
                error: function () {
                    //setTimeout(jqchat.proxy(this.startPopulating, this), 3000);
                    console.log("error in server call");
                    jQuery(".btn-start").removeClass("disabled");
                }
            });
        }

        function updateElementsContainer(elements) {
            var html = "";
            for (ie = 0; ie < elements.length; ie++) {
                var element = elements[ie];

                html += '<div class="row">';
                html += '  <div class="span1">';
                html += '    <label>' + element.name + '</label>';
                html += '  </div>';
                html += '  <div class="span6">';
                html += '    <div class="progress">';
                html += '      <div class="bar" style="width: ' + element.percentage + ';"></div>';
                html += '    </div>';
                html += '  </div>';
                html += '</div>';

                jQuery(".elements-container").html(html);

            }
        }

        var refreshInterval = setInterval(refresh, 3000);
    });

})(jQuery);