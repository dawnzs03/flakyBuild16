/**
*
* Photobooth.js version 0.7-rsd3
*
* build Wed Aug 14 2019 16:44:09 GMT-0300 (Brasilia Standard Time)
*
* CSS
*/
window.addEventListener("load",function(){var s = document.createElement("style"); s.innerHTML=".photobooth{position:relative;font:11px arial,sans-serif;overflow:hidden;user-select:none;-webkit-user-select:none;-moz-user-select:none;-o-user-select:none}.photobooth canvas{position:absolute;left:0;top:0}.photobooth .blind{position:absolute;left:0;top:0;opacity:0;width:100%;height:100%;background:#fff;z-index:1}.photobooth .blind.anim{transition:opacity 1500ms ease-out;-o-transition:opacity 1500ms ease-out;-moz-transition:opacity 1500ms ease-out;-webkit-transition:opacity 1500ms ease-out}.photobooth .warning{position:absolute;top:45%;background:#ffebeb;color:#cf0000;border:1px solid #cf0000;width:60%;left:50%;margin-left:-30%;display:none;padding:5px;z-index:10;text-align:center}.photobooth .warning span{text-decoration:underline;cursor:pointer;color:#333}.photobooth ul{width:30px;position:absolute;right:0;top:0;background:rgba( 0,0,0,.6 );height:230px;z-index:2;border-bottom-left-radius:5px;list-style:none;padding-left:0}.photobooth ul li{width:30px;height:38px;background-repeat:no-repeat;background-position:center center;cursor:pointer;position:relative}.photobooth ul li:hover{background-color:#aaa}.photobooth ul li.selected{background-color:#ccc}.photobooth ul.noHSB{height:120px}.photobooth ul.noFlip{height:190px}.photobooth ul.noHSB.noFlip{height:80px}.photobooth ul.noFlip li.flip,.photobooth ul.noHSB li.hue,.photobooth ul.noHSB li.saturation,.photobooth ul.noHSB li.brightness{display:none}.photobooth ul li.flip{background-image:url(data:image/jpeg;base64,iVBORw0KGgoAAAANSUhEUgAAABwAAAAWCAYAAADTlvzyAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAC4jAAAuIwF4pT92AAADyUlEQVRIx71Vz2ujVRQ95+WHaG0bWlCEcaHV7vyxsD8ogjSkiaIdmIUyigPiILhQRN2I8xc4UBU3IqjoxhlUrAsXbdPQlNGFLYOBbmutI64KbUem7aTJd4+LvJe+fKMuvavk+953z733nHMfFxYWcmYmSSIJAHTOEQAkqVgs3pvL5TYA9OG/46DZbD5cr9evSYKZKZPJdBL6vEmSWFaSAMgDwDmHAAYA2Wz2eQB/ttvtl0lCgjrf90Ymk/kin8+fBXDRzBTyBDAz6+QLQP4l2TkRChDJ05K+qdVqP3cA1T0vSWEa5XL5a4Cnzey9CAgAGJqSBLbb7WdJ3n0CTEXn8yTnkiQZW15ebgRAAEiSRM65UCSmp6cfzeVy65LeBNDyQAQgkvLgO6HVTQB/paYkAATw2+Li4tlQgSQkSSKSCN2RpCRVKpXLAO7rnIN6G8UAgAdoZjKzM9Vq9YcwijA2T3zIGYiXrzqmAL7wztjIIDz5XKpUKrMk57NxS/HIzKyr2vC6VCo945w7B2AUwG0Srkk2v7+//+Xa2trhyMiI29zctBjM54IvBNm02kgyjCzMY3x8/M5CoXCJ5BMAvpL0gaQ2yVHn3IWhoaHXK5XKxyTf2NraejCyWADqCjENyHg0zjn09fWxUCh8S/JUq9V6qFqtbpOk95gmJiYuDg4OfkLyfQC7KYX2WAwAXNyZf67YP1NTU+dIjh8fHz9Zq9W2AcA5103S39//tKSfANxIgXSp8Z7kLT6MFwC74V6U9OnKysofZhYrE2NjY3c4594GkAfwO4Dt0J2nxtLdZmNz+kDowhfQNLPvzawr1RBra2uHACbSnow3S/jvPdkBjPN47roPlpYWZ0PHXgw9uEHNMzMz485l3qpWl174t3UZd6g0WEhaLlcukbjfn7u5t7f31Pr6+mG0suCXwBlAd6U5PKGGvSoN38b7sdMZigCcpAsAbjQajcOYAgAoFov3kHzFzN7t4eWk8FtVGi3rHk/6nwWSj+3u7n7XarUY5UOpVDqVz+fnJW00Go3PQqFpatIj7ZFvRLQAHEr6kOT54eHhjVJp5nNAvwC83Tk+DuAlSVcPDg6e29nZsX/SQai/BzCScwCDJB0dHT2yurp6fXJy8qOBgYFXnXOzAN8BcADgqpm9duXKj5ebzZvxldYjsNjfXZVGe5PxgqjX69cB0FtgTtKcJAVPhm9j2iR1b/toZIo5DFs+fKyUTXqIT+/HOKKln16XPRyOlsvlyXABh/Nx5RIQpu6BFN8kvgiFaaYvYJKjAX0TwAj+n/j1b3e/jAKG43odAAAAAElFTkSuQmCC)}.photobooth ul li.hue{background-image:url(data:image/jpeg;base64,/9j/4AAQSkZJRgABAgAAZABkAAD/7AARRHVja3kAAQAEAAAAZAAA/+4ADkFkb2JlAGTAAAAAAf/bAIQAAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQICAgICAgICAgICAwMDAwMDAwMDAwEBAQEBAQECAQECAgIBAgIDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMD/8AAEQgACAAYAwERAAIRAQMRAf/EAHgAAQEAAAAAAAAAAAAAAAAAAAkIAQEAAwAAAAAAAAAAAAAAAAAKBggLEAAAAwQLAAAAAAAAAAAAAAAAMQZBAjQ4A3MEdMQFdQcICTkRAAEBBAcGBwAAAAAAAAAAABExAAEhElECMjMEBQlhwgNzFDgVNRY3CBgK/9oADAMBAAIRAxEAPwBGOKPmqmNdT5FD2YgarLO67OVueIqrxF2tI/1Kn0jjjKfFcJZEt+5BAUCAaKuw+ThT3vC0wbFof+U4Dnv3WGl8Pu47A8vecwabKy8ZRVNKFdF3dY72fztbVdFu67axelcfrPkYlPTutCW7qqYCkwDf/9k=)}.photobooth ul li.saturation{background-image:url(data:image/jpeg;base64,/9j/4AAQSkZJRgABAgAAZABkAAD/7AARRHVja3kAAQAEAAAAZAAA/+4ADkFkb2JlAGTAAAAAAf/bAIQAAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQICAgICAgICAgICAwMDAwMDAwMDAwEBAQEBAQECAQECAgIBAgIDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMD/8AAEQgACAAYAwERAAIRAQMRAf/EAGMAAAMAAAAAAAAAAAAAAAAAAAYICQEBAQEAAAAAAAAAAAAAAAAACAkKEAAABgMBAAAAAAAAAAAAAAAAwYIDMwZxAkQHEQABAgUFAAAAAAAAAAAAAAAAAQYxgQIyM3HBQgMH/9oADAMBAAIRAxEAPwAwo0rWdSFXHBYpnLZmWjVB/fLedIODu5Do81j1y2KE0CJlJA2uK5ZjtY2Kg//Z)}.photobooth ul li.brightness{background-image:url(data:image/jpeg;base64,/9j/4AAQSkZJRgABAgAAZABkAAD/7AARRHVja3kAAQAEAAAAZAAA/+4ADkFkb2JlAGTAAAAAAf/bAIQAAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQICAgICAgICAgICAwMDAwMDAwMDAwEBAQEBAQECAQECAgIBAgIDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMD/8AAEQgACAAYAwERAAIRAQMRAf/EAFcAAQAAAAAAAAAAAAAAAAAAAAoBAQAAAAAAAAAAAAAAAAAAAAAQAAAEBQUAAAAAAAAAAAAAAACxAwgBMXECBXJzBDQ1EQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwAcTWfR4GtIwC5mITxNUDgAYA0joY3aRKwB/9k=)}.photobooth ul li.crop{background-image:url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAICAYAAADjoT9jAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAEFJREFUeNpi/A8EDAjACMT/qUgzMCJZwMhAXQA2l4VGhsPNZKKR4XBfMMG8QiPASDcf0MIX/2FxgCJARRoMAAIMAK49Iv4yTUj5AAAAAElFTkSuQmCC)}.photobooth ul li.trigger{background-image:url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAASCAYAAABB7B6eAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAa9JREFUeNqc1M8rRFEUwPF5M4MhP8aPIiIS21lQk1Is5ceChZIdOytlI/+A7Ig/gGRhpYiNbKQsrBRFLPzYWJghNH7MjOd76qhr8m6vOfWpmffevefec987juu6AZ8RQhhBpJHJuT+CfsiEDo6wGjYeKMKn8b8Um/jCG2qQ0skjyOIWB9hFNyaN8bWSwGEHM5q9EVc6mUQ9YpjDHQbwoQkjuspDDKNEF9hjJDjFcoAEx653XEoJMYoVxNGBGPZRhzbL+HTYWLEtpO6V6EQ5kijTc7HFiwyssDwgyXsxhW8tkZSxAAksoj3n7P4G20hatviKE3RpqXKN4V5K4TE+IQ89WBI8ao0DFkP49krw+057xbyWxBY72LIdXsbjnlzf8/kRbtgSeO1APqonnwlu8tlBIYp9JojmkyCiX7Kf6MsngcSsvvO2aMZEPmcgEcea7ua/aNKGaC2RY0lwgTNsYwwNOlkrprGOJe2q/84vvegabdrrQyqomrSTyirHtbPKc+84x4L2qBazORi/s9KuC7QfBY3JC1UVBlGt16PallPap+Tas+7wWc8za1Ql8yPAAAzkXGo1lmDtAAAAAElFTkSuQmCC)}.photobooth .submenu{background:rgba( 0,0,0,.6 );position:absolute;width:100px;opacity:0;height:20px;padding:5px 10px;color:#fff;top:4px;left:-124px;border-radius:5px;-webkit-transition:opacity 500ms ease;-moz-transition:opacity 500ms ease;-o-transition:opacity 500ms ease;-msie-transition:opacity 500ms ease;transition:opacity 500ms ease}.photobooth li:hover .submenu{opacity:1}.photobooth .submenu .tip{width:4px;height:8px;position:absolute;right:-4px;top:50%;margin-top:-2px;background:url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAQAAAAICAYAAADeM14FAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAADVJREFUeNpiYGBgmAnEDP///wdjJgYImMnIyAhmwATggowwLTCArAKrQDqyQDrcMGQlAAEGAAGOCdflbyWyAAAAAElFTkSuQmCC)}.photobooth .submenu .slider{width:100px;height:20px;position:relative}.photobooth .submenu .slider .track{height:2px;width:100px;position:absolute;top:9px;background:rgba(255,255,255,.6)}.photobooth .submenu .slider .handle{height:14px;width:2px;position:absolute;top:3px;background:#fff;z-index:2}.photobooth .submenu .slider .handle div{position:absolute;z-index:3;width:20px;top:-3px;height:20px;cursor:w-resize;left:-9px}.resizehandle{position:absolute;z-index:1;width:100px;height:100px;left:30px;top:30px;cursor:move;outline:1500px solid rgba( 0,0,0,.35 );box-shadow:2px 2px 10px rgba(0,0,0,.5),0 0 3px #000;opacity:0;transition:opacity 500ms ease;-moz-transition:opacity 500ms ease;-o-transition:opacity 500ms ease;-webkit-transition:opacity 500ms ease}noindex:-o-prefocus,.resizehandle{outline:0!important}@-moz-document url-prefix(){.resizehandle{ box-shadow:none!important}}.resizehandle .handle{width:100%;height:100%;border:2px dashed #0da4d3;margin:-2px 0 0 -2px;z-index:3;position:relative}.resizehandle .handle div{width:18px;height:18px;position:absolute;right:-2px;bottom:-2px;z-index:4;cursor:se-resize;background-image:url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA8AAAAPCAYAAAA71pVKAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAHdJREFUeNpi/P//PwO5gIlcjXxLr/xnIlujsg7pNsM0AgEjE7kaSfIzusZ/d4n0M1aNxPgZWeMHC4RGIJuREV8847IRpBGvnwlpxBnPRGkEyYOcjYx5l1z+z3/8Pwij8NHlQWwUPxNrI4afSdUI9zNZGoF8gAADAOGvmx/e+CgVAAAAAElFTkSuQmCC);background-position:top left;background-repeat:no-repeat}"; document.head.appendChild(s);},false);
/**
* JS
*/
Photobooth=function(t){var e=this;t.length&&(t=t[0]);var i=navigator.mediaDevices.getUserMedia||navigator.getUserMedia||navigator.webkitGetUserMedia||navigator.mozGetUserMedia||navigator.oGetUserMedia||navigator.msieGetUserMedia||!1;this.onImage=function(){},this.getHueOffset=function(){return a},this.setHueOffset=function(t){v(t,"hue")&&(a=t)},this.getBrightnessOffset=function(){return c},this.setBrightnessOffset=function(t){v(t,"brightness")&&(c=t)},this.getSaturationOffset=function(){return r},this.setSaturationOffset=function(t){v(t,"saturation")&&(r=t)},this.setCrop=function(){S.toggle(),"crop"===B.className?B.className="crop selected":B.className="crop"},this.crop=function(t,e,i,n){S.cropMove(t,e),S.cropResize(i,n)},this.pause=function(){!1===h&&(h=!0,d&&d.stop?d.stop():d.getTracks().forEach(t=>t.stop()))},this.resume=function(){!0===h&&(h=!1,U())},this.destroy=function(){this.pause(),t.removeChild(w)},this.forceHSB=!0,this.flipCammera=!0,this.mimeType="image/png",this.quality=1,this.isSupported=!!i,this.resize=function(t,e){if(0==e&&(e=N.videoHeight/(N.videoWidth/t),isNaN(e)&&(e=t/(4/3))),t<200||e<200)throw"Error: Not enough space for Photobooth. Min height / width is 200 px";p=t,f=e,S.setMax(p,f),w.style.width=t+"px",w.style.height=e+"px",b.width=t,b.height=e,M.width=t,M.height=e,N.width=t,N.height=e},this.capture=function(){H.className="blind",H.style.opacity=1,setTimeout(function(){H.className="blind anim",H.style.opacity=0},50);var t={};t=S.isActive()?S.getData():l?{x:(p-N.videoWidth)/2,y:(f-N.videoHeight)/2,width:N.videoWidth,height:N.videoHeight}:{x:0,y:0,width:p,height:f};var e=y("canvas");if(e.width=t.width,e.height=t.height,l)e.getContext("2d").drawImage(N,Math.max(0,t.x-(p-N.videoWidth)/2),Math.max(t.y-(f-N.videoHeight)/2),t.width,t.height,0,0,t.width,t.height);else{var i=E.getImageData(t.x,t.y,t.width,t.height);e.getContext("2d").putImageData(i,0,0)}u.onImage(e.toDataURL(u.mimeType,u.quality))};var n=function(t){t.stopPropagation(),t.cancelBubble=!0},o=function(t){this.startX=0,this.startY=0,t.addEventListener("mousedown",this,!1)};o.prototype.onStart=function(t,e){},o.prototype.onMove=function(t,e){},o.prototype.onStop=function(t,e){},o.prototype.handleEvent=function(t){this["fon"+t.type](t)},o.prototype.fonmousedown=function(t){t.preventDefault(),this.startX=t.clientX,this.startY=t.clientY,this.onStart(this.startX,this.startY),document.addEventListener("mousemove",this,!1),document.addEventListener("mouseup",this,!1)},o.prototype.fonmousemove=function(t){this.onMove(t.clientX-this.startX,t.clientY-this.startY)},o.prototype.fonmouseup=function(t){this.onStop(t.clientX-this.startX,t.clientY-this.startY),document.removeEventListener("mousemove",this),document.removeEventListener("mouseup",this)};var s=function(t,e){t.innerHTML='<div class="submenu"><div class="tip"></div><div class="slider"><div class="track"></div><div class="handle" style="left:50px"><div></div></div></div></div>';var i=50,s=50,a=t.getElementsByClassName("handle")[0],r=t.getElementsByClassName("slider")[0],c=new o(a);c.onMove=function(t){l(i+t)},c.onStop=function(t){i=s};var l=function(t){t>0&&t<100&&(s=t,a.style.left=t+"px",e((t-50)/100))};r.addEventListener("click",function(t){l(t.layerX),i=s},!1),a.addEventListener("click",n,!1)},a=0,r=0,c=0,l=!1,h=!1,d=null,u=this,p=0===t.offsetWidth?$(t).width():t.offsetWidth,f=0===t.offsetHeight?$(t).height():t.offsetHeight,v=function(t,e){if(t<-.5||t>.5)throw"Invalid value: "+e+" must be between 0 and 1";return!0},m=window.requestAnimationFrame||window.webkitRequestAnimationFrame||window.mozRequestAnimationFrame||window.oRequestAnimationFrame||window.msRequestAnimationFrame||function(t){window.setTimeout(t,1e3/60)},g=function(t){return w.getElementsByClassName(t)[0]},y=function(t){return document.createElement(t)},w=y("div");w.className="photobooth",w.innerHTML='<div class="blind"></div><canvas></canvas><div class="warning notSupported">Sorry, Photobooth.js is not supported by your browser</div><div class="warning noWebcam">Please give Photobooth permission to use your Webcam. <span>Try again</span></div><ul><li title="flip"class="flip"></li><li title="hue"class="hue"></li><li title="saturation"class="saturation"></li><li title="brightness"class="brightness"></li><li title="crop"class="crop"></li><li title="take picture"class="trigger"></li></ul>';var b=y("canvas"),x=b.getContext("2d"),M=w.getElementsByTagName("canvas")[0],E=M.getContext("2d"),N=y("video");N.autoplay=!0;var L=g("noWebcam");L.getElementsByTagName("span")[0].onclick=function(){U()},new s(g("hue"),function(t){a=t}),new s(g("saturation"),function(t){r=t}),new s(g("brightness"),function(t){c=t});var S=new function(t,e,i){this.setMax=function(t,n){e=t,i=n},this.getData=function(){return{x:s,y:a,width:r,height:c}},this.cropMove=function(t,n){t+r<e&&t>0&&(l=t,f.style.left=l+"px"),n+c<i&&n>0&&(h=n,f.style.top=h+"px"),s=l,a=h},this.cropResize=function(t,n){s+t<e&&t>18&&(d=t,f.style.width=d+"px"),a+n<i&&n>18&&(u=n,f.style.height=u+"px"),r=d,c=u},this.isActive=function(){return p},this.toggle=function(){!1===p?(f.style.opacity=1,p=!0):(f.style.opacity=0,p=!1)};var s=30,a=30,r=100,c=100,l=30,h=30,d=100,u=100,p=!1,f=document.createElement("div");f.className="resizehandle",f.innerHTML='<div class="handle"><div></div></div>',t.appendChild(f);var v=f.getElementsByTagName("div")[0],m=new o(v);m.onMove=function(t,n){s+t+r<e&&s+t>0&&(l=s+t,f.style.left=l+"px"),a+n+c<i&&a+n>0&&(h=a+n,f.style.top=h+"px")},m.onStop=function(){s=l,a=h};var g=f.getElementsByTagName("div")[1];g.addEventListener("mousedown",n,!1);var y=new o(g);y.onMove=function(t,n){s+t+r<e&&r+t>18&&(d=r+t,f.style.width=d+"px"),a+n+c<i&&c+n>18&&(u=c+n,f.style.height=u+"px")},y.onStop=function(){r=d,c=u}}(w,p,f),B=g("crop");B.onclick=function(){S.toggle(),"crop"===B.className?B.className="crop selected":B.className="crop"};var H=g("blind");g("trigger").onclick=function(){e.capture()};var C=!1;g("flip").onclick=function(){C=!C,e.pause(),e.resume()};var T=function(t){if(d=t,D(),"object"==typeof N.srcObject)N.srcObject=d,!1===u.forceHSB?(l=!0,w.appendChild(N)):N.addEventListener("canplay",function(){m(I)},!1),N.play();else try{N.src=(window.URL||window.webkitURL).createObjectURL(d),m(I)}catch(t){N.mozSrcObject=d,!1===u.forceHSB?(l=!0,w.appendChild(N)):N.addEventListener("canplay",function(){m(I)},!1),N.play()}},k=function(t){L.style.display="block"},U=function(){var t={video:{facingMode:C?"user":"environment"}};L.style.display="none",navigator.mediaDevices.getUserMedia?navigator.mediaDevices.getUserMedia(t).then(T).catch(k):i.call(navigator,t,T,k)},D=function(){!1===u.forceHSB&&w.getElementsByTagName("ul")[0].classList.add("noHSB"),!1===u.flipCammera&&w.getElementsByTagName("ul")[0].classList.add("noFlip")},O=function(t,e,i){return i<0&&(i+=1),i>1&&(i-=1),i<1/6?t+6*(e-t)*i:i<.5?e:i<2/3?t+(e-t)*(2/3-i)*6:t},R=function(t){return t>1?1:t<0?0:t},I=function(){try{x.drawImage(N,0,0,p,f)}catch(t){}for(var t,e=x.getImageData(0,0,p,f),i=e.data,n=0;n<i.length;n+=4){var o,s,l=i[n]/255,d=i[n+1]/255,u=i[n+2]/255,v=Math.max(l,d,u),g=Math.min(l,d,u),y=(v+g)/2;if(v==g)o=s=0;else{var w=v-g;s=y>.5?w/(2-v-g):w/(v+g),v===l&&(o=((d-u)/w+(d<u?6:0))/6),v===d&&(o=((u-l)/w+2)/6),v===u&&(o=((l-d)/w+4)/6)}if(o=(t=o+a)>1?t-1:t<0?1+t:t,s=R(s+r),y=R(y+c),0===s)l=d=u=y;else{var b=y<.5?y*(1+s):y+s-y*s,M=2*y-b;l=O(M,b,o+1/3),d=O(M,b,o),u=O(M,b,o-1/3)}i[n]=255*l,i[n+1]=255*d,i[n+2]=255*u}E.putImageData(e,0,0),!1===h&&m(I)};D(),this.resize(p,f),t.appendChild(w),i?m(U):g("notSupported").style.display="block"};
/**
* jQuery integration. (It's safe to delete this line if you're not using jQuery)
*/
window.jQuery&&($.fn.photobooth=function(){return this.each(function(o,t){var n=new Photobooth(t);$(t).data("photobooth",n),n.onImage=function(o){$(t).trigger("image",o)}})});