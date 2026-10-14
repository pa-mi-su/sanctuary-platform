import { Component, computed, inject, signal } from '@angular/core';

import { NewsApiService, NewsArticle } from '../core/api/news-api.service';

interface MockStory {
  id: number | string;
  category: string;
  title: string;
  summary: string;
  image: string;
  alt: string;
  minutes: number;
  article?: NewsArticle;
}
const photo = (id: string, width = 1000) =>
  `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${width}&q=85`;

@Component({
  selector: 'app-news-page',
  standalone: true,
  styleUrl: './news-page.component.scss',
  template: `
    <div class="newspaper">
      @if (isMock) {
        <div class="preview-banner">
          <span class="preview-dot"></span> DESIGN PREVIEW
          <span class="preview-description">Fictional stories · Illustrative stock photos</span>
        </div>
      }
      <header class="masthead">
        <div class="edition">
          <span>THE CATHOLIC LIFE, EVERY DAY</span
          ><span>{{ isMock ? 'Sample edition' : 'Independent Catholic journalism' }}</span>
        </div>
        <div class="nameplate">
          <img src="brand-logo.png" alt="" />
          <div>
            <h1>Sanctuary</h1>
            <p>CATHOLIC NEWS</p>
          </div>
        </div>
        <p class="motto">Faith at the heart of the story.</p>
      </header>
      <nav class="sections" aria-label="News categories">
        @for (category of categories; track category) {
          <button
            type="button"
            [class.selected]="selectedCategory() === category"
            [attr.aria-pressed]="selectedCategory() === category"
            (click)="selectCategory(category)"
          >
            {{ category }}
          </button>
        }
      </nav>
      @if (selectedStory(); as story) {
        <article class="story-detail">
          <button class="back-link" type="button" (click)="closeStory()">
            ← Back to headlines
          </button>
          <p class="kicker">
            {{ story.category }} {{ isMock ? '· Fictional sample article' : '' }}
          </p>
          <h2>{{ story.title }}</h2>
          <p class="standfirst">{{ story.summary }}</p>
          <p class="byline">
            By {{ story.article?.author || 'the Sanctuary sample desk' }} · {{ story.minutes }} min
            read
          </p>
          @if (story.image) {
            <img [src]="story.image" [alt]="story.alt" />
            <p class="caption">
              {{
                story.article
                  ? story.article.imageCaption + ' ' + story.article.imageCredit
                  : 'Illustrative stock photograph via Unsplash. Not a photograph of the fictional event.'
              }}
            </p>
          }
          <div class="article-copy">
            @if (story.article; as article) {
              <div class="body-text">{{ article.body }}</div>
              @if (article.sources) {
                <h3>Sources</h3>
                @for (
                  source of article.sources.split(
                    '
'
                  );
                  track $index
                ) {
                  <p>
                    @if (isSourceLink(source)) {
                      <a [href]="source.trim()" target="_blank" rel="noopener noreferrer">{{
                        source
                      }}</a>
                    } @else {
                      {{ source }}
                    }
                  </p>
                }
              }
              @if (article.corrections.length) {
                <h3>Corrections</h3>
                @for (correction of article.corrections; track $index) {
                  <p>
                    {{ correction.note }} <small>({{ correction.createdAt }})</small>
                  </p>
                }
              }
            } @else {
              <p>
                This fictional sample previews the reading experience. Real articles will contain
                original reporting, attributed interviews, and verified sources.
              </p>
            }
          </div>
        </article>
      } @else {
        <div class="edition-heading">
          <h2>{{ selectedCategory() === 'Latest' ? 'The latest' : selectedCategory() }}</h2>
          <span>{{
            isMock ? 'Design preview · Three sample stories' : 'Original reporting from Sanctuary'
          }}</span>
        </div>
        @if (visibleStories(); as stories) {
          @if (!isMock && loading()) {
            <p role="status">Loading news…</p>
          }
          @if (!isMock && loadFailed()) {
            <p role="status">News is temporarily unavailable. Please try again later.</p>
          }
          @if (!isMock && !loading() && !loadFailed() && !stories.length) {
            <p>No published articles in this section yet. Check back soon.</p>
          }
          <section
            class="front-page"
            [class.single-story]="!homepage().side.length"
            [class.no-lead]="!homepage().lead"
            aria-label="Top stories"
          >
            @if (homepage().lead; as lead) {
              <article class="lead">
                <button class="story-button" type="button" (click)="selectedStory.set(lead)">
                  @if (lead.image) {
                    <img class="lead-photo" [src]="lead.image" [alt]="lead.alt" />
                  }
                  <span class="kicker"
                    >{{ lead.category }}
                    @if (isMock) {
                      <span> / SAMPLE STORY</span>
                    }
                  </span>
                  <h2>{{ lead.title }}</h2>
                  <p class="summary">{{ lead.summary }}</p>
                  <span class="byline"
                    >{{ lead.article?.author || 'Sanctuary sample desk' }} · {{ lead.minutes }} min
                    read</span
                  >
                </button>
              </article>
            }
            @if (homepage().side.length) {
              <div class="middle-column">
                @for (story of homepage().side; track story.id) {
                  <article>
                    <button class="story-button" type="button" (click)="selectedStory.set(story)">
                      @if (story.image) {
                        <img class="secondary-photo" [src]="story.image" [alt]="story.alt" />
                      }
                      <span class="kicker"
                        >{{ story.category }} {{ isMock ? '· SAMPLE' : '' }}</span
                      >
                      <h3>{{ story.title }}</h3>
                      <p class="summary">{{ story.summary }}</p>
                      <span class="byline">{{ story.minutes }} min read</span>
                    </button>
                  </article>
                }
              </div>
            }
          </section>
          @if (homepage().more.length) {
            <section class="more-section" aria-labelledby="more-heading">
              <div class="section-title"><h2 id="more-heading">More stories</h2></div>
              <div class="more-grid">
                @for (story of homepage().more; track story.id) {
                  <article>
                    <button class="story-button" type="button" (click)="selectedStory.set(story)">
                      @if (story.image) {
                        <img
                          class="more-photo"
                          [src]="story.image"
                          [alt]="story.alt"
                          loading="lazy"
                        />
                      }
                      <span class="kicker"
                        >{{ story.category }} {{ isMock ? '· SAMPLE' : '' }}</span
                      >
                      <h3>{{ story.title }}</h3>
                      <p class="summary">{{ story.summary }}</p>
                      <span class="byline">{{ story.minutes }} min read</span>
                    </button>
                  </article>
                }
              </div>
            </section>
          }
        }
        <footer class="news-footer">
          <strong>Sanctuary Catholic News</strong>
          <p>Original reporting. Thoughtful context. Catholic life.</p>
          @if (isMock) {
            <span
              >Preview only. All stories are fictional; photographs are illustrative. No external
              news feed.</span
            >
          }
        </footer>
      }
    </div>
  `,
})
export class NewsPageComponent {
  private readonly api = inject(NewsApiService);
  readonly isMock =
    typeof window !== 'undefined' &&
    new URLSearchParams(window.location.search).get('preview') === '1';
  readonly loading = signal(!this.isMock);
  readonly loadFailed = signal(false);
  constructor() {
    if (!this.isMock)
      this.api.list().subscribe({
        next: (articles) => {
          const stories = articles.map((a) => this.display(a));
          this.liveStories.set(stories);
          const selectedId = new URLSearchParams(window.location.search).get('article');
          if (selectedId)
            this.selectedStory.set(stories.find((story) => story.id === selectedId) || null);
          this.loading.set(false);
        },
        error: () => {
          this.loadFailed.set(true);
          this.loading.set(false);
        },
      });
  }
  private readonly liveStories = signal<MockStory[]>([]);
  private display(article: NewsArticle): MockStory {
    return {
      id: article.id || '',
      category: article.category,
      title: article.title,
      summary: article.summary,
      image: article.imageId ? this.api.image(article.imageId) : '',
      alt: article.imageAlt,
      minutes: Math.max(1, Math.ceil(article.body.split(/\s+/).length / 200)),
      article,
    };
  }
  isSourceLink(source: string): boolean {
    try {
      return new URL(source.trim()).protocol === 'https:';
    } catch {
      return false;
    }
  }
  readonly categories = ['Latest', 'Vatican', 'U.S. Church'];
  readonly selectedCategory = signal('Latest');
  readonly selectedStory = signal<MockStory | null>(null);
  readonly allStories: MockStory[] = [
    {
      id: 1,
      category: 'U.S. Church',
      title: 'When the church doors stay open, a neighborhood finds its gathering place',
      summary:
        'A sample look at the volunteers turning an ordinary parish hall into a place for meals, conversation, and belonging.',
      image: photo('photo-1438032005730-c779502df39b'),
      alt: 'Illustrative church architecture',
      minutes: 5,
    },
    {
      id: 2,
      category: 'Vatican',
      title: 'The beauty of slowing down: rediscovering the pilgrimage',
      summary:
        'Beyond the destination, a journey on foot offers time for prayer and a different way of seeing.',
      image: photo('photo-1470770841072-f978cf4d019e'),
      alt: 'Illustrative quiet landscape beside water',
      minutes: 4,
    },
    {
      id: 3,
      category: 'U.S. Church',
      title: 'A seat at the table for the next generation',
      summary: 'What might it look like to build parish life around listening to young adults?',
      image: photo('photo-1511632765486-a01980e01a18'),
      alt: 'Illustrative family gathering outdoors',
      minutes: 3,
    },
  ];
  readonly visibleStories = computed(() =>
    (this.isMock ? this.allStories : this.liveStories()).filter(
      (story) => this.selectedCategory() === 'Latest' || story.category === this.selectedCategory(),
    ),
  );
  readonly homepage = computed(() => {
    const stories = this.visibleStories();
    if (this.isMock) return { lead: stories[0], side: stories.slice(1, 3), more: stories.slice(3) };
    const lead = stories.find((story) => story.article?.placement === 'LEAD');
    const side = ['SIDEBAR_1', 'SIDEBAR_2']
      .map((slot) => stories.find((story) => story.article?.placement === slot))
      .filter((story): story is MockStory => !!story);
    const featured = new Set([lead?.id, ...side.map((story) => story.id)]);
    return { lead, side, more: stories.filter((story) => !featured.has(story.id)) };
  });
  closeStory(): void {
    this.selectedStory.set(null);
    // Returning to a section must not reopen an old article when the page is refreshed.
    if (typeof window !== 'undefined') {
      const url = new URL(window.location.href);
      url.searchParams.delete('article');
      window.history.replaceState({}, '', `${url.pathname}${url.search}${url.hash}`);
    }
  }
  selectCategory(category: string): void {
    this.selectedCategory.set(category);
    this.closeStory();
  }
}
