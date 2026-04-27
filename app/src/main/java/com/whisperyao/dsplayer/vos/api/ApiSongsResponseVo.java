package com.whisperyao.dsplayer.vos.api;

import com.whisperyao.dsplayer.datasource.network.vo.BaseVo;
import java.util.List;

/* loaded from: classes2.dex */
public class ApiSongsResponseVo extends BaseVo {
    List<SongVo> songs;

    public static class SongVo {
        private AdditionalVo additional;
        private String id;
        private boolean isPersonal;
        private String path;
        private String title;
        private String type;
        private String url;

        public String getId() {
            return this.id;
        }

        public String getType() {
            return this.type;
        }

        public String getPath() {
            return this.path;
        }

        public String getUrl() {
            return this.url;
        }

        public String getTitle() {
            return this.title;
        }

        public boolean isPersonal() {
            return this.isPersonal;
        }

        public int getRating() {
            AdditionalVo additionalVo = this.additional;
            if (additionalVo == null || additionalVo.song_rating == null) {
                return -1;
            }
            return this.additional.song_rating.rating;
        }

        public AdditionalVo getAdditionalVo() {
            return this.additional;
        }

        public static class AdditionalVo {
            private SongAudioVo song_audio;
            private SongRatingVo song_rating;
            private SongTagVo song_tag;

            public static class SongRatingVo {
                int rating;
            }

            public SongRatingVo getSongRatingVo() {
                return this.song_rating;
            }

            public SongTagVo getSongTagVo() {
                return this.song_tag;
            }

            public SongAudioVo getSongAudioVo() {
                return this.song_audio;
            }

            public static class SongTagVo {
                private String album;
                private String album_artist;
                private String artist;
                private String comment;
                private String composer;
                private int disc;
                private String genre;
                private int track;
                private int year;

                public String getAlbum() {
                    return this.album;
                }

                public String getArtist() {
                    return this.artist;
                }

                public String getAlbumArtist() {
                    return this.album_artist;
                }

                public String getComposer() {
                    return this.composer;
                }

                public String getGenre() {
                    return this.genre;
                }

                public int getDisc() {
                    return this.disc;
                }

                public int getTrack() {
                    return this.track;
                }

                public int getYear() {
                    return this.year;
                }

                public String getComment() {
                    return this.comment;
                }
            }

            public static class SongAudioVo {
                private long bitrate;
                private int channel;
                private String codec;
                private String container;
                private int duration;
                private long filesize;
                private int frequency;

                public int getDuration() {
                    return this.duration;
                }

                public int getFrequency() {
                    return this.frequency;
                }

                public long getBitRate() {
                    return this.bitrate;
                }

                public int getChannel() {
                    return this.channel;
                }

                public long getFileSize() {
                    return this.filesize;
                }

                public String getCodec() {
                    String str = this.codec;
                    return str == null ? "" : str;
                }

                public String getContainer() {
                    String str = this.container;
                    return str == null ? "" : str;
                }
            }
        }
    }

    public SongVo getSong(int index) {
        List<SongVo> list = this.songs;
        if (list == null || list.size() <= index) {
            return null;
        }
        return this.songs.get(index);
    }

    public List<SongVo> getSongs() {
        return this.songs;
    }
}