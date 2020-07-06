"use strict";
function retrieveCookies(details) {
    return [].concat.apply([], details.requestHeaders
        .filter((header) => header.name === 'Cookie')
        .map((cookies) => cookies.value.split('; ')))
        .map((cookieValueLine) => {
            let [k, v] = cookieValueLine.split("=")
            return ({name: k, value: v})
        })
}

function injectHeaders(details) {

    let headersFromCookies = retrieveCookies(details).filter((hd) => hd.name.startsWith("drill-"));
    return {requestHeaders: [...details.requestHeaders, ...(headersFromCookies)]};
}

browser.webRequest.onBeforeSendHeaders.addListener(
    injectHeaders,
    {urls: ['*://*/*']},
    ["blocking",
        "requestHeaders"
        // ,
        // "extraHeaders"
    ]);


