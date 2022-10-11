// ==UserScript==
// @name         Amazon Scraper
// @namespace    https://www.joehxtees.com
// @version      0.2
// @description  scrape data from Amazon proper
// @author       JoeHx
// @match        https://www.amazon.com/*
// @icon         https://www.google.com/s2/favicons?sz=64&domain=amazon.com
// @grant        none
// ==/UserScript==

(function() {
    'use strict';

    let div = document.createElement('div');
    div.style = 'position: fixed; top: 100px; left: 250px; z-index: 1000;';

    let textBox = document.createElement('input');
    textBox.type = "text"

    let button = document.createElement('button');
    button.textContent = "Scrape!";

    button.onclick = () => {
        let urls = JSON.parse(textBox.value);

        Promise.all(urls.map(url =>
          fetch(url)
            .then(resp => resp.text())
            .then(html => new DOMParser().parseFromString(html, 'text/html'))
            .then(doc => {
              let image = doc.querySelector('#landingImage');
              let title = image.alt;
              let imageSrc = image.getAttribute('data-old-hires');
              let bullets = Array.from(doc.querySelectorAll('#feature-bullets li')).map(node => node.textContent.trim())

              return [title, bullets, imageSrc, url];
            })
            .catch(function (err) {
              // There was an error
              console.warn('Something went wrong.', err);
            })
         )
       )
       .then(JSON.stringify)
       .then(json => {
            console.log(json);
            textBox.value = json;
        });
    }

    div.append(button);
    div.append(textBox);

    document.body.append(div);
})();