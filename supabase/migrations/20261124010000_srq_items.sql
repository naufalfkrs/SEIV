-- =====================================================================
-- SRQ-20: simpan jawaban per butir agar skor subskala bisa dihitung
-- (pedoman SRQ-20 / Riskesdas): depresi 6,9,10,14,15,16,17 · cemas 3,4,5 ·
-- somatik 1,2,7,19 · kognitif 8,12,13 · penurunan energi 8,11,12,13,18,20.
-- Disimpan sebagai bitmask 20 bit: bit (n-1) = 1 bila butir n dijawab "Ya".
-- =====================================================================
alter table public.screenings
    add column srq_items integer check (srq_items between 0 and 1048575);

comment on column public.screenings.srq_items is 'Bitmask jawaban SRQ-20: bit (n-1) = butir n dijawab Ya (null pada hasil lama)';
