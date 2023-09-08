(ns real-world-clojure-api.routes.htmx.shared.picture)

(defn random-picture
  []
  (let [skin-color ["Tanned"
                    "Yellow"
                    "Pale"
                    "Light"
                    "Brown"
                    "DarkBrown"
                    "Black"]
        top-type #{"Hat"
                   "LongHairNotTooLong"
                   "ShortHairDreads01"
                   "ShortHairShortFlat"
                   "ShortHairDreads02"
                   "LongHairFrida"
                   "WinterHat4"
                   "Turban"
                   "LongHairShavedSides"
                   "ShortHairTheCaesarSidePart"
                   "ShortHairShaggyMullet"
                   "ShortHairShortWaved"
                   "ShortHairFrizzle"
                   "LongHairMiaWallace"
                   "WinterHat2"
                   "LongHairBigHair"
                   "Hijab"
                   "LongHairStraightStrand"
                   "LongHairFroBand"
                   "ShortHairSides"
                   "NoHair"
                   "ShortHairShortRound"
                   "WinterHat1"
                   "LongHairDreads"
                   "ShortHairTheCaesar"
                   "LongHairFro"
                   "LongHairBun"
                   "WinterHat3"
                   "LongHairCurvy"
                   "Eyepatch"
                   "LongHairStraight"
                   "LongHairStraight2"
                   "ShortHairShortCurly"
                   "LongHairBob"
                   "LongHairCurly"}
        mouth-type #{"Tongue" "Default" "Smile" "Grimace" "Twinkle" "Disbelief" "Eating" "Sad" "Serious" "Concerned" "ScreamOpen" "Vomit"}
        hair-color #{"Platinum" "Black" "BlondeGolden" "BrownDark" "SilverGray" "Blue" "Brown" "Blonde" "Red" "PastelPink" "Auburn"}]
    (format "https://avataaars.io/?skinColor=%s&topType=%s&hairColor=%s&mouthType=%s"
            (first (shuffle skin-color))
            (first (shuffle top-type))
            (first (shuffle hair-color))
            (first (shuffle mouth-type)))))
