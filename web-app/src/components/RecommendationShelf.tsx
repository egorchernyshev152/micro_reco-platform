import MovieCard from './MovieCard';
import { Movie } from '../api/movieService';
import './shelf.css';

interface Props {
  title: string;
  movies: Movie[];
  loading?: boolean;
}

const RecommendationShelf = ({ title, movies, loading = false }: Props) => {
  const showPlaceholder = loading && movies.length === 0;
  if (!movies.length && !loading) return null;

  return (
    <section className="shelf">
      <div className="shelf__header">
        <h2>{title}</h2>
        <span>›</span>
      </div>

      <div className="shelf__grid">
        {showPlaceholder
          ? Array.from({ length: 4 }).map((_, index) => <div key={`skeleton-${index}`} className="shelf__placeholder" />)
          : movies.slice(0, 4).map((movie) => <MovieCard key={movie.id} movie={movie} />)}
      </div>
    </section>
  );
};

export default RecommendationShelf;
