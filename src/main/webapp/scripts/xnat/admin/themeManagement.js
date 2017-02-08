/*
 * web: themeManagement.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */
var themeUrl = XNAT.url.rootUrl('/xapi/theme');
var s = '/', q = '?', a = '&';
var csrf = 'XNAT_CSRF=' + window.csrfToken;
$('#titleAppName').text(XNAT.app.siteId);
var currentTheme = $('#currentTheme');
var themeSelector = $('#themeSelection');
var themeUploadForm = document.getElementById('themeFileUpload-form');
var themeUploader = document.getElementById('themeFileUpload-input');
var themeUploadSubmit = document.getElementById('themeFileUpload-button');
var selectedTheme = null;
function populateThemes(){
    getCurrentTheme(getAvailableThemes);
}
function getCurrentTheme(callback){
    var role = 'global';
    $.get(themeUrl + s + role, null, function(data){
        themeSelector.empty();
        selectedTheme = data.name ? data.name : 'None';
        currentTheme.text(selectedTheme);
        if (typeof callback === 'function') {
            callback(data.name);
        }
    }, 'json');
}
function getAvailableThemes(selected){
    $.get(themeUrl, null, function(data){
        themeSelector.empty();
        addThemeOptions(data, selected);
    }, 'json');
}
function addThemeOptions(newThemeOptions, selected){
    if (Array.isArray(newThemeOptions)) {
        $(newThemeOptions).each(function(i, opt){
            var select = '';
            if (selected == opt.value) {
                select = ' selected="selected"';
            }
            themeSelector.append('<option value="' + opt.value + '"' + select + '>' + opt.label + '</option>');
        });
    }
}
function selectTheme(themeToSelect){
    if (themeToSelect && typeof themeToSelect === 'string') {
        themeSelector.val(themeToSelect);
    }
}
var themeSelectionForm = $('#themeSelection').closest('form');
themeSelectionForm.off('submit');
themeSelectionForm.submit(function(ev){
    ev.preventDefault();
    xmodal.confirm({
        content: 'Theme selection appearances may not fully take effect until users log out, clear their browser cache and log back in.' +
        '<br><br>Are you sure you wish to change the global theme?',
        action: function(){
            $.put(themeUrl + s + encodeURI(themeSelector.val()) + q + csrf, null, function(data){
                console.log(data);
                populateThemes()
            });
        }
    });
});
function removeTheme(){
    xmodal.confirm({
        content: 'Are you sure you wish to delete the selected theme?',
        action: function(){
            $.delete(themeUrl + s + encodeURI(themeSelector.val()) + q + csrf, null, function(data){
                console.log(data);
                populateThemes();
            });
        }
    });
}

/*** Theme Package Upload Functions ***/
themeUploadForm.action = themeUrl + q + csrf;
$(themeUploadForm).parent().parent().css('position', 'relative');
$(themeUploadForm).parent().parent().css('top', '-30px');
themeUploadForm.onsubmit = function(event){
    event.preventDefault();
    $(themeUploadSubmit).text('Uploading...');
    $(themeUploadSubmit).attr('disabled', 'disabled');
    var files = themeUploader.files;
    var formData = new FormData();
    var uploaded = false;
    for (var i = 0; i < files.length; i++) {
        var file = files[i];
        if (!file.type.match('zip.*')) {
            continue;
        }
        formData.append('themePackage', file, file.name); // formData.append('themes[]', file, file.name);
        var xhr = new XMLHttpRequest();
        xhr.open('POST', themeUploadForm.action, true);
        xhr.onload = function(){
            if (xhr.status !== 200) {
                console.log(xhr.statusText);
                console.log(xhr.responseText);
                xmodal.message('Upload Error', 'There was a problem uploading your theme package.<br>Server responded with: ' + xhr.statusText);
            }
            $(themeUploadSubmit).text('Upload');
            $(themeUploadSubmit).removeAttr('disabled');
            var newThemeOptions = $.parseJSON(xhr.responseText);
            var selected;
            if (newThemeOptions[0]) {
                selected = newThemeOptions[0].value;
            }
            selected = null; // don't change the menu?
            addThemeOptions(newThemeOptions, selected);
        };
        xhr.send(formData);
        uploaded = true;
    }
    if (!uploaded) {
        xmodal.message('Nothing Uploaded', 'No valid theme package files were selected for upload.<br><br>Click the "Choose Files" button below to browse for a theme package.');
        $(themeUploadSubmit).text('Upload');
        $(themeUploadSubmit).removeAttr('disabled');
    }
    else {
        XNAT.ui.banner.top(2000, 'Theme uploaded.', 'success');
    }
    return false;
};

$('body').on('change', '#themeSelection', function(){
    var THEME = this.value;
    var URL = XNAT.url.csrfUrl('/xapi/theme/' + THEME);
    XNAT.xhr.put(URL).done(function(){
        XNAT.ui.banner.top(2000, 'Theme set to "' + THEME + '".', 'success');
    })
});

$(populateThemes);  // ...called once DOM is fully loaded "ready"
