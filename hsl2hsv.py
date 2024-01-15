# Copied code.

hue = int(input("Hue: "))
saturation = float(input("Saturation: "))
lightness = float(input("Lightness: "))

s = saturation / 100
l = lightness / 100

# Convert to HSV.
v = l + s * min(l, 1 - l)
if v == 0:
    hsv_saturation = 0
else:
    hsv_saturation = 2 * (1 - l / v)

# Adjust HSV values to required ranges.
hsv_hue = hue / 2 # Scale hue to 0 - 179.
hsv_saturation *= 255 # Scale saturation to 0 - 255.
v *= 255 # Scale value to 0 - 255.

print(f"HSV: {round(hsv_hue)} {round(hsv_saturation)} {round(v)}")
