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

    const urls = ["https://www.amazon.com/dp/B0B5GG1VYV","https://www.amazon.com/dp/B0B5FN5M3N","https://www.amazon.com/dp/B0B41229GF","https://www.amazon.com/dp/B0B41R272J","https://www.amazon.com/dp/B0B2J7CYSZ","https://www.amazon.com/dp/B0B2JK1LPD","https://www.amazon.com/dp/B0B2HWG5PH","https://www.amazon.com/dp/B0B2HTGSZS","https://www.amazon.com/dp/B0B2BGKSYP","https://www.amazon.com/dp/B0B284NSPB","https://www.amazon.com/dp/B09ZQ64N8W","https://www.amazon.com/dp/B09ZQ8PZTL","https://www.amazon.com/dp/B09QMBKXNM","https://www.amazon.com/dp/B09QLHHN8V","https://www.amazon.com/dp/B09QH72JBG","https://www.amazon.com/dp/B09QH2YCWX","https://www.amazon.com/dp/B09QB1NPPW","https://www.amazon.com/dp/B09PV1H3DK","https://www.amazon.com/dp/B09MVY4F9N","https://www.amazon.com/dp/B09MB6HS7C","https://www.amazon.com/dp/B09JMP2Q1Y","https://www.amazon.com/dp/B09J7LYMC8","https://www.amazon.com/dp/B09J4NS55L","https://www.amazon.com/dp/B09D8WSD68","https://www.amazon.com/dp/B098KXPJN2","https://www.amazon.com/dp/B09895MYWV","https://www.amazon.com/dp/B097GLLRFY","https://www.amazon.com/dp/B0979WH6B2","https://www.amazon.com/dp/B0971W76B8","https://www.amazon.com/dp/B0971RWWD2","https://www.amazon.com/dp/B0971W5WPP","https://www.amazon.com/dp/B094T6RZ4V","https://www.amazon.com/dp/B091F1YGXK","https://www.amazon.com/dp/B091C7GXHK","https://www.amazon.com/dp/B091BZMYXH","https://www.amazon.com/dp/B08YY1G4V9","https://www.amazon.com/dp/B08YXX96RJ","https://www.amazon.com/dp/B08YXWQV4C","https://www.amazon.com/dp/B08XXKD54Q","https://www.amazon.com/dp/B08VCX2MFQ","https://www.amazon.com/dp/B08NM6XCB6","https://www.amazon.com/dp/B08M6FJWCF","https://www.amazon.com/dp/B08LNCH2YV","https://www.amazon.com/dp/B08KJWK1TQ","https://www.amazon.com/dp/B08KH92TZC","https://www.amazon.com/dp/B08KHGCH1V","https://www.amazon.com/dp/B08J2F1Z4Q","https://www.amazon.com/dp/B08HK7C191","https://www.amazon.com/dp/B08HGFZQS8","https://www.amazon.com/dp/B08GDG8J22","https://www.amazon.com/dp/B08GDHYCXN","https://www.amazon.com/dp/B08G9T9RNP","https://www.amazon.com/dp/B08G5PHZ3W","https://www.amazon.com/dp/B08G2G6XQL","https://www.amazon.com/dp/B08FY6LMB7","https://www.amazon.com/dp/B08FSTKVQ9","https://www.amazon.com/dp/B08DYTYBC1","https://www.amazon.com/dp/B08DXCQTW8","https://www.amazon.com/dp/B08CMRY2SB","https://www.amazon.com/dp/B086D2SBQY","https://www.amazon.com/dp/B086D77Q5D","https://www.amazon.com/dp/B086CYFJ2W","https://www.amazon.com/dp/B086DFJZYY","https://www.amazon.com/dp/B0869HV5J4","https://www.amazon.com/dp/B0869HJV5Q","https://www.amazon.com/dp/B0869GLSP3","https://www.amazon.com/dp/B0869FJQY4","https://www.amazon.com/dp/B084542JP8","https://www.amazon.com/dp/B08453RGFF","https://www.amazon.com/dp/B0842WMWC1","https://www.amazon.com/dp/B0842WK9GG","https://www.amazon.com/dp/B0842WHLY1","https://www.amazon.com/dp/B0843C1GFQ","https://www.amazon.com/dp/B08424QBD3","https://www.amazon.com/dp/B08424PBFJ","https://www.amazon.com/dp/B082SK8QWC","https://www.amazon.com/dp/B0826N8HWH","https://www.amazon.com/dp/B0826NYBRP","https://www.amazon.com/dp/B08211VL5D","https://www.amazon.com/dp/B081ZX6ZY8","https://www.amazon.com/dp/B081DZ7LT5","https://www.amazon.com/dp/B0812HG4BW","https://www.amazon.com/dp/B0811N8S9V","https://www.amazon.com/dp/B07ZZQ6X3F","https://www.amazon.com/dp/B07ZMRLBLY","https://www.amazon.com/dp/B07ZL6PC86","https://www.amazon.com/dp/B07ZBVXFH7","https://www.amazon.com/dp/B07ZDWMRNC","https://www.amazon.com/dp/B07ZBVRVKJ","https://www.amazon.com/dp/B07ZB3D2VD","https://www.amazon.com/dp/B07YXVCTBV","https://www.amazon.com/dp/B07YXVC1ZG","https://www.amazon.com/dp/B07YXS6FWF","https://www.amazon.com/dp/B07YS56TV6","https://www.amazon.com/dp/B07YQMGY4P","https://www.amazon.com/dp/B07YQMBWNW","https://www.amazon.com/dp/B07YQMM9PJ","https://www.amazon.com/dp/B07YQMMR6W","https://www.amazon.com/dp/B07YNKW6XN","https://www.amazon.com/dp/B07YNLMW8W"];

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