// ==UserScript==
// @name         MBA Scraper
// @namespace    https://www.joehxtees.com
// @version      0.2
// @description  scrape merch URLs from merch.amazon.com
// @author       You
// @match        https://merch.amazon.com/manage/designs
// @icon         https://www.google.com/s2/favicons?sz=64&domain=amazon.com
// @grant        none
// ==/UserScript==

let urlArray;
let urlStrings;

(function() {
    'use strict';

    let div = document.createElement('div');
    div.style = 'position: fixed; top: 100px; left: 250px; z-index: 1000;';

    let textBox = document.createElement('input');
    textBox.type = "text"

    let numberBox = document.createElement('input');
    numberBox.type = "number"

    let button = document.createElement('button');

    button.textContent = "Scrape!";

    button.onclick = () => {
        urlArray = Array.from(document.querySelectorAll('table.table a')).map(a => a.href);

        if (numberBox.value > 0) {
            urlArray = urlArray.slice(0, numberBox.value);
        }

        urlStrings = JSON.stringify(urlArray);

        textBox.value = urlStrings;
        console.log(urlStrings);
    }

    div.append(button);
    div.append(numberBox);
    div.append(textBox);

    document.body.append(div);
})();