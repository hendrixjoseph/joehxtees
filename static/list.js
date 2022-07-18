let filter = value => {
  Array.from(document.querySelectorAll('.shirt'))
    .forEach(shirt => shirt.hidden = !
      [shirt.querySelector('h2').textContent,
       Array.from(shirt.querySelectorAll('li')).slice(0,-1).map(a => a.textContent).join('')]
      .join('').toLowerCase().includes(value.toLowerCase())
    );
}

let inputFilter = input => {
  filter(input.value);
  
  window.location.hash = input.value;
}

window.onload = () => {
  let value = window.location.hash.slice(1);
  filter(value);
  document.querySelector('input').value = value;
}