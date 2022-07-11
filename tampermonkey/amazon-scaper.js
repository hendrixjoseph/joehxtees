// ==UserScript==
// @name         Amazon Scraper
// @namespace    https://www.joehxtees.com
// @version      0.1
// @description  scrape data from Amazon proper
// @author       JoeHx
// @match        https://www.amazon.com/dp/*
// @icon         https://www.google.com/s2/favicons?sz=64&domain=amazon.com
// @grant        none
// ==/UserScript==

(function() {
    'use strict';

    const urls = [];

    let button = document.createElement('button');
    button.style = 'position: fixed; top: 100px; left: 250px; z-index: 1000;';
    button.textContent = "Scrape!";

    button.onclick = () => {
        Promise.all(urls.map(url =>
          fetch(url)
            .then(resp => resp.text())
            .then(html => new DOMParser().parseFromString(html, 'text/html'))
            .then(doc => {
              let image = doc.querySelector('#landingImage');
              let title = image.alt;
              let imageSrc = image.getAttribute('data-old-hires');

              let bullets = doc.querySelectorAll('#feature-bullets li');
              let bullet = [bullets[3].textContent.trim()];

              if (bullets.length === 6) {
                bullet.push(bullets[4].textContent.trim());
              }

              return [title, bullet, imageSrc, url];
            })
            .catch(function (err) {
              // There was an error
              console.warn('Something went wrong.', err);
            })
         )
       )
       .then(JSON.stringify)
       .then(console.log);
    }

    document.body.append(button);
})();