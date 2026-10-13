import math, random
from PIL import Image, ImageDraw, ImageFilter
OUT='/tmp/Khaos-Nexus-Mod/src/main/resources/assets/khaosnexus/textures/'
S=4  # supersample
def fin(img,size):
    img=img.resize((size,size),Image.LANCZOS)
    return img.filter(ImageFilter.UnsharpMask(radius=1,percent=60,threshold=2))
random.seed(7)
# frame: dark obsidian stone with carved rune grooves
def frame(glow=False):
    n=32*S; im=Image.new('RGBA',(n,n),(0,0,0,0) if glow else (28,22,38,255)); d=ImageDraw.Draw(im)
    if not glow:
        for _ in range(900):
            x,y=random.randrange(n),random.randrange(n); c=random.randint(18,52)
            d.rectangle([x,y,x+S*2,y+S*2],fill=(c,c-6,c+14,255))
        d.rectangle([0,0,n-1,n-1],outline=(12,10,18,255),width=S*2)
        d.rectangle([S*3,S*3,n-1-S*3,n-1-S*3],outline=(58,50,74,255),width=S)
    col=(255,255,255,255) if glow else (14,10,20,255)
    w=S*3
    c=n//2
    d.polygon([(c,S*7),(n-S*7,c),(c,n-S*7),(S*7,c)],outline=col,width=w)
    d.line([(c,S*11),(c,n-S*11)],fill=col,width=w); d.line([(S*11,c),(n-S*11,c)],fill=col,width=w)
    d.ellipse([c-S*3,c-S*3,c+S*3,c+S*3],outline=col,width=w)
    return fin(im,32)
frame().save(OUT+'block/nexus_gate_frame.png',optimize=True)
g=frame(True); a=g.getchannel('A').point(lambda v: min(255,v*3)); g.putalpha(a); g.save(OUT+'block/nexus_gate_frame_glow.png',optimize=True)
# closed variant frame = cracked & dim
cl=frame().convert('RGBA'); cl=Image.blend(cl,Image.new('RGBA',cl.size,(40,40,40,255)),0.35)
cl.save(OUT+'block/nexus_gate_closed.png',optimize=True)
frame().save(OUT+'block/nexus_gate.png',optimize=True)
# portal: animated swirl, grayscale/white (tinted by rank in renderer)
F=16; N=32; strip=Image.new('RGBA',(N,N*F))
n=N*S
for f in range(F):
    t=f/F*2*math.pi
    im=Image.new('RGBA',(n,n)); px=im.load()
    for y in range(n):
        for x in range(n):
            u=(x/n-.5)*2; v=(y/n-.5)*2
            r=math.hypot(u,v); a=math.atan2(v,u)
            s=math.sin(a*3+r*9-t*2)*.5+.5
            s2=math.sin(a*-2+r*14+t*3)*.5+.5
            b=(.35+.45*s*s+.25*s2**4)*(1.15-.5*min(r,1.4))
            core=max(0,1-r*1.6)**2
            b=min(1,b+core)
            g=int(255*b); px[x,y]=(g,g,g,int(170+85*b))
    strip.paste(fin(im,N),(0,N*f))
strip.save(OUT+'block/nexus_gate_portal.png',optimize=True)
open(OUT+'block/nexus_gate_portal.png.mcmeta','w').write('{"animation":{"frametime":2,"interpolate":true}}\n')
# item icon: mini gate
n=16*S; im=Image.new('RGBA',(n,n)); d=ImageDraw.Draw(im)
d.rectangle([S*2,0,n-S*2-1,n-1],fill=(30,24,42,255),outline=(10,8,16,255),width=S)
d.rectangle([S*5,S*3,n-S*5-1,n-S*3-1],fill=(205,60,80,255))
d.ellipse([S*6,S*5,n-S*6-1,n-S*5-1],fill=(255,200,210,255))
fin(im,16).save(OUT+'item/nexus_gate.png',optimize=True)
