(ns dagga-bay.products
  "Hard-coded South African cannabis product catalog.")

(def products
  [{:id "dp-001"
    :name "Durban Poison"
    :category :flower
    :strain-type :sativa
    :thc 22
    :description "Pure South African sativa landrace. Uplifting, creative, energetic. The legendary strain from the port city of Durban."
    :weights [{:g 1 :price 120} {:g 3.5 :price 380} {:g 7 :price 700} {:g 14 :price 1250} {:g 28 :price 2200}]
    :featured true}

   {:id "sg-002"
    :name "Swazi Gold"
    :category :flower
    :strain-type :sativa
    :thc 18
    :description "Classic Swazi landrace. Smooth, mellow high with a golden hue. Grown in the mountains of Eswatini for generations."
    :weights [{:g 1 :price 100} {:g 3.5 :price 320} {:g 7 :price 580} {:g 14 :price 1050} {:g 28 :price 1900}]
    :featured true}

   {:id "pp-003"
    :name "Power Plant"
    :category :flower
    :strain-type :sativa
    :thc 20
    :description "South African sativa powerhouse. Strong cerebral buzz with sweet, earthy flavour. A Cape Town favourite."
    :weights [{:g 1 :price 110} {:g 3.5 :price 350} {:g 7 :price 650} {:g 14 :price 1150} {:g 28 :price 2050}]
    :featured true}

   {:id "mk-004"
    :name "Malawi Gold"
    :category :flower
    :strain-type :sativa
    :thc 16
    :description "Rare African landrace from Malawi. Long flowering, incredibly smooth smoke with a psychedelic edge."
    :weights [{:g 1 :price 130} {:g 3.5 :price 400} {:g 7 :price 750} {:g 14 :price 1350} {:g 28 :price 2400}]}

   {:id "rr-005"
    :name "Rooibaard (Red Beard)"
    :category :flower
    :strain-type :indica
    :thc 24
    :description "Heavy-hitting Cape indica. Deep body relaxation with notes of spice and earth. Perfect for evening use."
    :weights [{:g 1 :price 140} {:g 3.5 :price 430} {:g 7 :price 800} {:g 14 :price 1450} {:g 28 :price 2600}]}

   {:id "ct-006"
    :name "Cape Town Kush"
    :category :flower
    :strain-type :indica
    :thc 26
    :description "Premium Table Mountain indica. Dense buds, purple hues, heavy trichome coverage. Deep relaxation."
    :weights [{:g 1 :price 150} {:g 3.5 :price 460} {:g 7 :price 850} {:g 14 :price 1550} {:g 28 :price 2800}]
    :featured true}

   {:id "gh-007"
    :name "Garden Route Haze"
    :category :flower
    :strain-type :hybrid
    :thc 21
    :description "Balanced hybrid cultivated along the Garden Route. Creative focus with gentle body calm. 60/40 sativa-dominant."
    :weights [{:g 1 :price 125} {:g 3.5 :price 390} {:g 7 :price 720} {:g 14 :price 1300} {:g 28 :price 2300}]}

   {:id "kn-008"
    :name "KZN Purple"
    :category :flower
    :strain-type :hybrid
    :thc 19
    :description "Beautiful purple hybrid from KwaZulu-Natal. Berry flavours, balanced high, gorgeous bag appeal."
    :weights [{:g 1 :price 135} {:g 3.5 :price 410} {:g 7 :price 770} {:g 14 :price 1400} {:g 28 :price 2500}]}

   {:id "ws-009"
    :name "Wild Dagga Blend"
    :category :flower
    :strain-type :hybrid
    :thc 15
    :description "Unique hybrid blended with Leonotis leonurus terpene profile. Traditional meets modern. Gentle, social high."
    :weights [{:g 1 :price 90} {:g 3.5 :price 280} {:g 7 :price 500} {:g 14 :price 900} {:g 28 :price 1600}]}

   ;; ── EDIBLES ──────────────────────────────────

   {:id "ed-010"
    :name "Table Mountain Gummies"
    :category :edibles
    :strain-type :hybrid
    :thc 10
    :description "Assorted fruit gummies, 10mg THC each. 10-pack. Lab tested, precisely dosed. Made in Cape Town."
    :weights [{:g 100 :price 250}]
    :unit "pack"}

   {:id "ed-011"
    :name "Rooibos THC Tea"
    :category :edibles
    :strain-type :indica
    :thc 5
    :description "Authentic SA rooibos infused with 5mg THC per bag. 20 bags per box. Relaxing evening ritual."
    :weights [{:g 1 :price 350}]
    :unit "box"}

   {:id "ed-012"
    :name "Biltong Bites (Infused)"
    :category :edibles
    :strain-type :sativa
    :thc 15
    :description "THC-infused beef biltong snacks, 15mg per 50g pack. Braai-ready. Proudly South African."
    :weights [{:g 50 :price 200} {:g 100 :price 380}]
    :unit "pack"}

   {:id "ed-013"
    :name "Koeksister Caramels"
    :category :edibles
    :strain-type :hybrid
    :thc 8
    :description "Hand-made caramel chews inspired by koeksisters. 8mg THC each, 10-pack. Sweet & sticky."
    :weights [{:g 100 :price 220}]
    :unit "pack"}

   ;; ── VAPES ────────────────────────────────────

   {:id "vp-014"
    :name "Durban Poison Cartridge"
    :category :vapes
    :strain-type :sativa
    :thc 85
    :description "Pure Durban Poison distillate. 510-thread cartridge, 1ml. Lab-tested, solvent-free."
    :weights [{:g 1 :price 450}]
    :unit "cart"
    :featured true}

   {:id "vp-015"
    :name "Cape Kush Live Resin Pod"
    :category :vapes
    :strain-type :indica
    :thc 78
    :description "Live resin pod from Cape Town Kush. Full-spectrum terpenes, maximum flavour. Proprietary pod system."
    :weights [{:g 0.5 :price 380} {:g 1 :price 650}]
    :unit "pod"}

   {:id "vp-016"
    :name "Garden Route Disposable"
    :category :vapes
    :strain-type :hybrid
    :thc 70
    :description "All-in-one disposable vape. Garden Route Haze strain. ~300 puffs. Rechargeable USB-C."
    :weights [{:g 1 :price 350}]
    :unit "device"}

   ;; ── CONCENTRATES ─────────────────────────────

   {:id "cn-017"
    :name "Durban Poison Shatter"
    :category :concentrates
    :strain-type :sativa
    :thc 88
    :description "Glass-like shatter from premium Durban Poison. Clean extraction, amber clarity. Dab-ready."
    :weights [{:g 0.5 :price 300} {:g 1 :price 550}]
    :unit "gram"}

   {:id "cn-018"
    :name "Cape Rosin Press"
    :category :concentrates
    :strain-type :indica
    :thc 75
    :description "Solventless rosin pressed from Cape Town Kush flower. Full melt, terpene-rich. Artisan craft."
    :weights [{:g 0.5 :price 350} {:g 1 :price 650}]
    :unit "gram"}

   ;; ── ACCESSORIES ──────────────────────────────

   {:id "ac-019"
    :name "Dugout One-Hitter"
    :category :accessories
    :strain-type nil
    :thc 0
    :description "Hand-carved wooden dugout with brass one-hitter. Discreet, portable, classic. Made in Woodstock, CT."
    :weights [{:g 1 :price 180}]
    :unit "piece"}

   {:id "ac-020"
    :name "Rolling Kit (Premium)"
    :category :accessories
    :strain-type nil
    :thc 0
    :description "Complete kit: RAW papers, glass tips, pocket tray, grinder card. Gift-ready packaging."
    :weights [{:g 1 :price 250}]
    :unit "kit"}

   {:id "ac-021"
    :name "Glass Bong — Table Mountain"
    :category :accessories
    :strain-type nil
    :thc 0
    :description "30cm borosilicate glass bong with Table Mountain silhouette design. Ice-catcher, 14mm bowl."
    :weights [{:g 1 :price 650}]
    :unit "piece"}

   {:id "ac-022"
    :name "Herb Grinder (4-piece)"
    :category :accessories
    :strain-type nil
    :thc 0
    :description "CNC aluminium 4-piece grinder. 63mm, sharp diamond teeth, kief catcher. Matte black."
    :weights [{:g 1 :price 320}]
    :unit "piece"}])

(def categories
  [{:id :flower       :label "🌿 Flower"       :emoji "🌿"}
   {:id :edibles      :label "🍫 Edibles"      :emoji "🍫"}
   {:id :vapes        :label "💨 Vapes"         :emoji "💨"}
   {:id :concentrates :label "💎 Concentrates"  :emoji "💎"}
   {:id :accessories  :label "🔧 Accessories"   :emoji "🔧"}])

(def strain-types
  [{:id :sativa  :label "Sativa"  :color "#39ff14"}
   {:id :indica  :label "Indica"  :color "#ff6b9d"}
   {:id :hybrid  :label "Hybrid"  :color "#ffa726"}])
