// ==UserScript==
// @name         MBA Scraper
// @namespace    https://www.joehxtees.com
// @version      0.1
// @description  scrape merch URLs from merch.amazon.com
// @author       You
// @match        https://merch.amazon.com/manage/designs
// @icon         https://www.google.com/s2/favicons?sz=64&domain=amazon.com
// @grant        none
// ==/UserScript==

(function() {
    'use strict';

    let button = document.createElement('button');
    button.style = 'position: fixed; top: 100px; left: 250px; z-index: 1000;';
    button.textContent = "Scrape!";

    button.onclick = () => {
        console.log(
            JSON.stringify(
                Array.from(document.querySelectorAll('table.table a'))
                    .map(a => a.href)
            )
        )
    }

    document.body.append(button);
})();