// interactions with 'Security Passwords' section of admin ui
console.log('passwordExpirationType.js');

(function(){

    var container$ = $('div[data-name="passwordExpiration"]');
    var bundles$ = container$.find('div.input-bundle');


    // Password Expiration
    (function(){

        var fieldInterval$ =
                container$.find('#passwordExpirationInterval')
                    .css({ marginTop: '10px' });

        var fieldDate$ =
                container$.find('#passwordExpirationDate')
                    .attr('placeholder', 'MM/DD/YYYY')
                    .css({
                        marginTop: '10px',
                        width:     '90px'
                    })
                    .datetimepicker({
                        timepicker: false,
                        // today is max date, disallow future date selection
                        // maxDate:    '+1970/01/01',
                        format:     'm/d/Y'
                    });

        var openCal$ =
                container$.find('#openCal-passwordExpirationDate')
                    .click(function(){
                        fieldDate$.datetimepicker('show');
                    });

        container$.find('input[name="passwordExpirationType"]').on('change', function(){
            changeExpirationType(this.value);
        });

        changeExpirationType(XNAT.data.siteConfig.passwordExpirationType);

        function changeExpirationType(value){
            value = (value || 'interval').toLowerCase();
            bundles$.hide();
            bundles$.filter('.' + value).show();
        }

    })();


    // Password Reuse
    (function(){

        var durationContainer$ = $('div[data-name="passwordHistoryDuration"]');
        var durationInput$ = durationContainer$.find('input#passwordHistoryDuration');

        $('#passwordReuseRestriction').on('change', function(){
            changePasswordReuseType(this.value);
        });

        changePasswordReuseType(XNAT.data.siteConfig.passwordReuseRestriction);

        function changePasswordReuseType(value){
            value = (value || 'none').toLowerCase();
            if (value === 'historical'){
                durationContainer$.removeClass('disabled');
                durationInput$.prop('disabled', false).removeClass('disabled');
            }
            else {
                durationContainer$.addClass('disabled');
                durationInput$.prop('disabled', true).addClass('disabled');
            }
        }

    })();


})();
